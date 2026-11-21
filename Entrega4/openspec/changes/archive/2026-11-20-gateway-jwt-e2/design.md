# Diseño: seguridad JWT en el gateway (Entrega 2)

## Decisión general

Se adopta **OAuth2 Resource Server con RS256**. El gateway valida tokens, no
los genera. Se conserva la estructura del proyecto de referencia
(`SecurityFilterChain` stateless, `securityMatcher("/api/**")`, matriz por path,
claim de roles), pero se reemplaza JJWT + base `auth` + secreto hardcodeado por
el starter oficial de Spring Security.

```
msvc-usuarios (8081)  --RS256-->  JWT { idUsuario, roles }
        |                              |
        | POST /api/usuarios/login     |
        v                              v
Cliente ----------------------> msvc-gateway (8080)
                                  SecurityFilterChain  valida firma RS256 / exp / iss / aud
                                  hasRole(...)          autoriza por path
                                  IdentityHeadersFilter descarta X-User-* del cliente
                                                        setea X-User-Id desde idUsuario
                                                        elimina X-User-Role
                                        |
                                        v  lb://msvc-* (Eureka)
                              servicios de dominio (leen solo X-User-Id)
```

### Aclaración: `SecurityFilterChain`, no `SecurityWebFilterChain`

El gateway usa `spring-cloud-starter-gateway-server-webmvc`, es decir el modelo
**servlet/MVC**. La clase correcta es
`org.springframework.security.web.SecurityFilterChain`.
`SecurityWebFilterChain` corresponde a WebFlux/reactivo y **no aplica** acá.
La cadena se construye con `HttpSecurity`, no con `ServerHttpSecurity`.

## Emisión y validación RS256

- `msvc-usuarios` genera el par RSA y firma el JWT con la clave privada.
- La **clave privada se entrega solo por variable de entorno**
  (`JWT_PRIVATE_KEY`, PEM con saltos de línea escapados), nunca como archivo del
  repositorio ni como valor literal en YAML.
- El gateway valida con la clave pública configurada con
  `public-key-location: ${JWT_PUBLIC_KEY_LOCATION}` (archivo montado o
  `classpath:`). **No se usa JWKS**: no se publica `/.well-known/jwks.json` y el
  gateway no agrega `spring-security-oauth2-jose`.
- El contrato completo (claims `iss`, `aud`, `iat`, `exp`, `idUsuario` y
  `roles`, duración de 900 segundos y generación PKCS#8 de la clave) está en
  **ADR-0003**. El claim de roles se llama `roles` en emisor y validador; el
  prefijo `ROLE_` se agrega solo en el gateway.
- La audiencia se lee de `${JWT_AUDIENCE}` y el issuer de `${JWT_ISSUER}`,
  ambos obligatorios.
- `.env.example` documenta las variables con valores de ejemplo; `.env` sigue
  ignorado por Git y no se versiona.

## Configuración del gateway

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          public-key-location: ${JWT_PUBLIC_KEY_LOCATION}
          audiences: ${JWT_AUDIENCE}
          issuer: ${JWT_ISSUER}
```

Sin esas variables el `JwtDecoder` no puede construirse y el gateway no arranca.
Es el comportamiento esperado en Entrega 2: **fail closed**, no un fallback a la
identidad simulada de Entrega 1.

## `SecurityConfig.java` (esquema)

Ubicación: `com.ticketya.gateway.config.SecurityConfig`, según el `package-info`
de `gateway/config`. La matriz, el error writer y el filtro siguen en
`gateway/security`.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName("idUsuario"); // antes: sub
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GatewayRoleMatrix roleMatrix,
            GatewaySecurityErrorWriter writer,
            JwtAuthenticationConverter converter
    ) throws Exception {

        http
            .securityMatcher("/api/**", "/internal/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> {
                // Contratos internos nunca accesibles desde el gateway.
                authz.requestMatchers("/internal/**").denyAll();
                // Público: login, registro y consultas de eventos/artistas.
                authz.requestMatchers(HttpMethod.POST, "/api/usuarios/login", "/api/usuarios/registro").permitAll();
                authz.requestMatchers(HttpMethod.GET, "/api/eventos/**", "/api/artistas/**").permitAll();
                roleMatrix.apply(authz);
                authz.anyRequest().hasRole(GatewayRoles.CLIENTE);
            })
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req, res, e) -> writer.write(
                        res, req, HttpStatus.UNAUTHORIZED,
                        "Token ausente, invalido o vencido."))
                .accessDeniedHandler((req, res, e) -> writer.write(
                        res, req, HttpStatus.FORBIDDEN,
                        "No tenes el rol requerido para este recurso."))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> writer.write(
                        res, req, HttpStatus.UNAUTHORIZED,
                        "Token ausente, invalido o vencido."))
                .accessDeniedHandler((req, res, e) -> writer.write(
                        res, req, HttpStatus.FORBIDDEN,
                        "No tenes el rol requerido para este recurso."))
            );

        return http.build();
    }
}
```

Notas:

- `setPrincipalClaimName("idUsuario")` es el fix del punto urgente: **no**
  usar `jwt.getSubject()` porque el token no trae `sub`.
- `roles` → `ROLE_CLIENTE`, `ROLE_ADMINISTRADOR`, etc. Se usa `hasRole("CLIENTE")`
  (Spring ya agrega el prefijo `ROLE_`).
- `securityMatcher("/api/**", "/internal/**")` deja `/error` y `/actuator/**`
  fuera de la cadena, evitando que el `GatewayErrorController` termine
  devolviendo 401. `/internal/**` se agrega solo para `denyAll`: aunque alguien
  enrute un contrato interno, el gateway no lo sirve.
- El fallback `anyRequest().hasRole(GatewayRoles.CLIENTE)` cubre rutas no
  declaradas; la prueba de matriz falla si aparece una ruta en el YAML sin una
  regla explícita en `GatewayRoleMatrix`.
- No se usa YAML declarativo para autorización: las reglas viven en Java,
  según el `config.yaml`.

## `IdentityHeadersFilter.java` (esquema)

```java
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER + 1) // posterior a Security
public class IdentityHeadersFilter extends OncePerRequestFilter {

    private final GatewaySecurityErrorWriter writer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response);   // /error, actuator: sin identidad
            return;
        }

        String idUsuario = jwtAuth.getToken().getClaimAsString("idUsuario");
        if (idUsuario == null || idUsuario.isBlank()) {
            writer.write(response, request, HttpStatus.UNAUTHORIZED,
                    "El token no contiene el claim idUsuario.");
            return;                               // fail closed, nunca propaga null
        }

        HttpServletRequest seguro = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) return idUsuario;
                if ("X-User-Role".equalsIgnoreCase(name)) return null; // eliminado
                return super.getHeader(name);
            }
            // Sobrescribir tambien getHeaders(...) y getHeaderNames() para que
            // no queden valores duplicados enviados por el cliente.
        };

        chain.doFilter(seguro, response);
    }
}
```

La validación del claim también se puede hacer en el `JwtAuthenticationConverter`
(lanzando `OAuth2AuthenticationException`, que el entry point convierte en 401).
Se recomienda **ambos**: converter como guardia primaria y filtro como guardia
defensiva. Solo se escribe la respuesta una vez.

## `GatewaySecurityErrorWriter.java`

Reutiliza `GatewayErrorResponse` (`status`, `error`, `message`, `path`) para que
401 y 403 no rompan el contrato del gateway.

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "No tenes el rol requerido para este recurso.",
  "path": "/api/entradas"
}
```

## Matriz rol → path y separación de endpoints

Roles del seed de `msvc-usuarios`: `CLIENTE`, `ORGANIZADOR`, `ADMINISTRADOR`,
`PERSONAL_ACCESO`.

### Regla gruesa por path

| Path | Roles |
|---|---|
| `POST /api/usuarios/login` | público (`permitAll`) |
| `POST /api/usuarios/registro` | público (`permitAll`) |
| `/api/usuarios/**`, `/api/roles/**` | `ADMINISTRADOR` |
| `/api/admin/usuarios/**` | `ADMINISTRADOR` |
| `/api/lugares/**`, `/api/sectores/**` | `ADMINISTRADOR`, `ORGANIZADOR` |
| `GET /api/eventos/**` (consulta, incluye disponibilidad) | público (`permitAll`) |
| `/api/organizador/eventos/**` (propuesto) | `ORGANIZADOR` |
| `/api/admin/eventos/**` (propuesto) | `ADMINISTRADOR` |
| `GET /api/artistas/**` (consulta) | público (`permitAll`) |
| `/api/admin/artistas/**` (alta y edición) | `ADMINISTRADOR` |
| `/api/entradas/**` (propuesto: cliente) | `CLIENTE` |
| `/api/admin/entradas/**` (propuesto) | `ADMINISTRADOR` |
| `/api/accesos/**` (propuesto: QR) | `PERSONAL_ACCESO` |
| `/api/pagos/**` | `CLIENTE` |
| `/api/preferencias/**`, `/api/recomendaciones/**` | `ADMINISTRADOR`, `CLIENTE` |

Las reglas públicas `POST /api/usuarios/login`, `POST /api/usuarios/registro`,
`GET /api/eventos/**` y `GET /api/artistas/**` se evalúan **antes** de
`GatewayRoleMatrix.apply(...)`. `GET /api/eventos/**` cubre también
`GET /api/eventos/{idEvento}/disponibilidad`; la matriz no debe volver a exigir
roles sobre esos prefijos.

En la API, **“artista” designa a cualquier participante de un evento** (artista,
equipo o disertante). La entidad `Participante` conserva el atributo `tipo`
(texto libre) para distinguirlos. El path público se mantiene como
`/api/artistas/**`; no se renombra a `/api/participantes`.

Se adopta mapear los prefijos nuevos en el `@RequestMapping` del controller, sin
depender de `RewritePath` en el gateway.

### ⚠️ Endpoints con comportamiento distinto por rol dentro del mismo servicio

Estos son los que te tengo que avisar antes de implementar, porque **no se
pueden resolver con una sola regla por path** sin separarlos:

**`msvc-entrada` — hoy todos bajo `/api/entradas/**`:**

| Endpoint | Rol que lo exige hoy (`exigirRol`) | Destino final |
|---|---|---|
| `POST /api/entradas` | CLIENTE | `/api/entradas/**` (CLIENTE) |
| `GET /api/entradas` | CLIENTE | `/api/entradas/**` (CLIENTE) |
| `GET /api/entradas/{id}` | CLIENTE | `/api/entradas/**` (CLIENTE) |
| `POST /api/entradas/{id}/devolucion` | CLIENTE | `/api/entradas/**` (CLIENTE) |
| `PUT /api/entradas/{id}/devolucion` | ADMINISTRADOR | `/api/admin/entradas/**` (ADMINISTRADOR) |
| `POST /api/entradas/{id}/devolucion/reintentar` | ADMINISTRADOR | `/api/admin/entradas/**` (ADMINISTRADOR) |
| `POST /api/entradas/eventos/{idEvento}/cancelacion` | ADMINISTRADOR | `/api/admin/entradas/**` (ADMINISTRADOR) |
| `POST /api/entradas/accesos/validar` | PERSONAL_ACCESO | `POST /api/accesos/escanear` (PERSONAL_ACCESO) |
| `GET /api/entradas/disponibilidad` | sin check | `GET /api/eventos/{idEvento}/disponibilidad` (público) |
| `PUT /api/entradas/{id}`, `DELETE /api/entradas/{id}` | sin check | **eliminados** |

Decisiones cerradas:

- `GET /api/eventos/{idEvento}/disponibilidad?idSector=...` expone solo
  `idEvento`, `idSector`, `capacidad`, `ocupadas` y `disponibles`; no incluye
  compradores ni datos personales. Verificado en
  `EntradaService.disponibilidad`.
- El contrato Feign interno de `msvc-evento` deja de usar
  `GET /api/entradas/disponibilidad` y pasa a
  `GET /internal/entradas/disponibilidad` en `msvc-entrada`; el gateway no
  enruta `/internal/**`.
- `PUT/DELETE /api/entradas/{id}` se eliminan: se verificó que ningún servicio
  los invoca y las bajas ya están cubiertas por devolución y cancelación de
  evento. Si apareciera una compensación, se movería a `/internal/**` sin ruta
  en el gateway.
- `POST /api/accesos/escanear` queda con `PERSONAL_ACCESO` y el gateway rutea
  `/api/accesos/**` a `msvc-entrada`.

**`msvc-evento` — valida roles por Feign a `UsuarioClient` (`validarRol`):**

| Operación | Rol |
|---|---|
| `crear`, `actualizar`, `configurarSectores`, `publicar`, `eliminarFisicamente` | ORGANIZADOR (+ dueño del evento) |
| `suspender`, `cambiarEstadoAdministrativo`, `cancelar`, `reintentarCoordinacionCancelacion` | ADMINISTRADOR |
| `validarPropietarioOAdministrador` | ADMINISTRADOR u ORGANIZADOR dueño |
| consultas/búsqueda | sin check de rol |

Propuesta: sacar `validarRol`/`tieneRol` de `EventoServiceImpl` (el gateway ya
autorizó) y conservar solo la comparación `evento.getIdOrganizador()` contra el
`X-User-Id` recibido. `EntradaClient.cancelarEntradas(...)` deja de enviar el
parámetro `rol`. El nuevo `GET /api/eventos/{idEvento}/disponibilidad` delega en
`EntradaClient.consultarDisponibilidad(...)` y se sirve públicamente desde
`msvc-evento`.

**`msvc-entrada/pagos` y `registros-pago`:** todos los checks son CLIENTE, se
resuelven con una sola regla `/api/pagos/**`. `registros-pago` no tiene ruta
pública y sigue igual.

## Usuarios, login y emisión de tokens

- `POST /api/usuarios/login` es público y recibe `email` + `password`. El password
  se almacena con BCrypt y, si las credenciales son válidas, `msvc-usuarios`
  emite el JWT RS256 con los claims `idUsuario` y `roles`.
- `POST /api/usuarios/registro` es público, pero **solo crea usuarios
  `CLIENTE`**: el DTO no expone `rol` y el servicio fuerza ese rol. Un campo
  `rol` enviado por el cliente no puede elevar privilegios.
- `ADMINISTRADOR`, `ORGANIZADOR` y `PERSONAL_ACCESO` se cargan por seed o por
  `POST /api/admin/usuarios`, protegido con `ADMINISTRADOR` en el gateway.
- La clave privada de firma vive solo en `JWT_PRIVATE_KEY`; la pública se
  entrega al gateway mediante `JWT_PUBLIC_KEY_LOCATION`.

## Cambios en los servicios de dominio

- Quitar `@RequestHeader("X-User-Role")` y los helpers `exigirRol`,
  `exigirRolCliente` de `EntradaController`, `PagoController` y
  `RegistroPagoController`.
- Quitar el parámetro `rol` de `EntradaClient.cancelarEntradas(...)` y de la
  llamada en `EventoServiceImpl`.
- Quitar `validarRol`/`tieneRol` por Feign en `EventoServiceImpl`; mantener la
  verificación de pertenencia por `idUsuario`.
- Mantener `@RequestHeader("X-User-Id") Long idUsuario` en los endpoints que
  hoy lo usan.
- Alinear el controller de participantes con `/api/artistas` para consultas y con
  `/api/admin/artistas` para alta y edición, conservando `tipo` para distinguir
  artista, equipo o disertante. No renombrar el path a `/api/participantes`.
- No se tocan DTOs, reglas de negocio ni paths de las APIs públicas salvo los
  cambios aprobados: disponibilidad bajo `/api/eventos/**`, baja de
  `PUT/DELETE /api/entradas/{id}`, escaneo bajo `/api/accesos/**`,
  consulta de artistas bajo `/api/artistas/**` y administración bajo
  `/api/admin/artistas/**` y `/api/admin/usuarios/**`.

## Diferencias con el proyecto de referencia

La diferencia principal **no es el tipo de cadena**: ambos usan
`SecurityFilterChain` con `HttpSecurity`, `securityMatcher("/api/**")`, CSRF
deshabilitado, `STATELESS` y `requestMatchers(...)`. La diferencia es la
**responsabilidad**: en el proyecto de referencia el gateway **emite y valida**
el token; en TicketYa el gateway **solo valida** un token emitido por
`msvc-usuarios`.

| Referencia | TicketYa | Motivo |
|---|---|---|
| `/api/authenticate` + `TokenProvider.createToken(...)` | `POST /api/usuarios/login` en `msvc-usuarios` | No se crea `msvc-auth`; la entidad `Usuario` ya vive ahí |
| JJWT + `SECRET` hardcodeado, HS256 | OAuth2 Resource Server + RS256 con claves por entorno | Prohíbe secretos versionados; separa firma y verificación |
| `JwtFilter` artesanal + `TokenProvider.validateToken(...)` | `oauth2ResourceServer().jwt()` con el `JwtDecoder` estándar | Se usa el starter oficial en vez de parseo manual |
| `PasswordEncoder` bean en el gateway | BCrypt en `msvc-usuarios` | El gateway no autentica credenciales ni tiene base de usuarios |
| `httpBasic(Customizer.withDefaults())` | Solo `Authorization: Bearer <token>` | No hay `UserDetailsService` en el gateway |
| `hasAuthority(AuthotityConstant._ALUMNO)` | `hasRole(...)` usando `GatewayRoleMatrix` | El claim `roles` se mapea a authorities `ROLE_*` |
| `principal = claims.getSubject()` | `setPrincipalClaimName("idUsuario")` | El token de TicketYa no trae `sub` |
| Sin control anti-spoofing explícito | `IdentityHeadersFilter` fija `X-User-Id` y elimina `X-User-Role` | El cliente no puede suplantar la identidad |
| `498` para token expirado | 401/403 con `GatewayErrorResponse` | Contrato uniforme de errores del gateway |
| `@EnableMethodSecurity` | Matriz centralizada por path | La autorización vive solo en el gateway |
| `spring-cloud-starter-gateway-mvc` | `spring-cloud-starter-gateway-server-webmvc` | El gateway de TicketYa es MVC/servlet |

## Pruebas previstas (documentadas, no implementadas en este paso)

- Sin token → 401 con JSON uniforme.
- Token inválido, vencido o con firma distinta → 401.
- Token sin claim `idUsuario` → 401, nunca `X-User-Id: null`.
- Rol insuficiente → 403.
- Cliente envía `X-User-Id` propio → se ignora; `X-User-Role` → no llega al destino.
- Matriz completa con `@ParameterizedTest`.
- Cada endpoint accesible solo por los roles correspondientes, sobre el entorno
  levantado con docker-compose.
- Prueba de carga del flujo de compra con JMeter: usuarios concurrentes,
  tiempos de respuesta y errores, con al menos 2 instancias de `msvc-entrada`.

## Riesgos y pendientes

- La emisión RS256, el password BCrypt y el alta de roles privilegiados se
  implementaron en `msvc-usuarios` y quedaron documentados en
  `docs/verificacion-seguridad-rutas.md`.
- La regla "acceso solo por gateway" requiere red/bind, no solo código.
- `gateway-entrega1` quedó archivado como `2026-11-20-gateway-entrega1`; la spec
  `gateway-ruteo` vigente está en `openspec/specs/gateway-ruteo/spec.md`.
- `GatewayErrorHandlingTest` fue reemplazado en un commit separado porque el
  requisito cambió por ADR-0001: ya no asume `X-User-Role` y pasó a verificar
  identidad desde el claim `idUsuario` y anti-spoofing.
