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

- `msvc-usuarios` genera un par de claves RSA y firma el JWT con la privada.
  La clave privada se entrega por variable de entorno o archivo montado, nunca
  versionada.
- El gateway valida con la clave pública. Dos alternativas:

| Opción | Config en el gateway | Requisito en `msvc-usuarios` |
|---|---|---|
| A (recomendada) JWKS | `jwk-set-uri: ${JWT_JWK_SET_URI}` | publicar `/.well-known/jwks.json` (nueva dependencia `spring-security-oauth2-jose`, requiere ADR) |
| B (más simple) clave pública | `public-key-location: ${JWT_PUBLIC_KEY_LOCATION}` | exponer la clave pública como archivo montado en el contenedor |

En ambos casos se configura además `audiences: ${JWT_AUDIENCE}` y, si aplica,
`issuer-uri` o `issuer`. La decisión A/B queda pendiente; la plantilla de los
YAML debe reescribirse para RS256 (la actual aún dice `jwk-set-uri` pensada en
un IdP externo).

## Configuración del gateway

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Opción A (recomendada):
          jwk-set-uri: ${JWT_JWK_SET_URI}
          # Opción B, alternativa:
          # public-key-location: ${JWT_PUBLIC_KEY_LOCATION}
          audiences: ${JWT_AUDIENCE}
```

Sin esas variables el `JwtDecoder` no puede construirse y el gateway no arranca.
Es el comportamiento esperado en Entrega 2: **fail closed**, no un fallback a la
identidad simulada de Entrega 1.

## `SecurityConfig.java` (esquema)

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
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> {
                // El login debe ser público y va ANTES de la regla de /api/usuarios/**.
                authz.requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll();
                roleMatrix.apply(authz);
                authz.anyRequest().authenticated();
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
- `securityMatcher("/api/**")` deja `/error` y `/actuator/**` fuera de la cadena,
  evitando que el `GatewayErrorController` termine devolviendo 401.
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
| `POST /api/usuarios/login` | público (permitAll) |
| `/api/usuarios/**`, `/api/roles/**` | `ADMINISTRADOR` |
| `/api/lugares/**`, `/api/sectores/**` | `ADMINISTRADOR`, `ORGANIZADOR` |
| `/api/eventos/**` (consulta) | `ADMINISTRADOR`, `ORGANIZADOR`, `CLIENTE` |
| `/api/organizador/eventos/**` (propuesto) | `ORGANIZADOR` |
| `/api/admin/eventos/**` (propuesto) | `ADMINISTRADOR` |
| `/api/artistas/**` | `ADMINISTRADOR`, `ORGANIZADOR`, `CLIENTE` |
| `/api/entradas/**` (propuesto: cliente) | `CLIENTE` |
| `/api/admin/entradas/**` (propuesto) | `ADMINISTRADOR` |
| `/api/accesos/**` (propuesto: QR) | `PERSONAL_ACCESO` |
| `/api/pagos/**` | `CLIENTE` |
| `/api/preferencias/**`, `/api/recomendaciones/**` | `ADMINISTRADOR`, `CLIENTE` |

Los prefijos nuevos requieren una de dos opciones: (a) mapear ambos paths en el
`@RequestMapping` del controller, o (b) agregar rutas en el gateway con
`RewritePath` para quitar el prefijo antes de reenviar. Se prefiere (a) para no
depender del soporte de filtros de reescritura del starter MVC; decisión
pendiente.

### ⚠️ Endpoints con comportamiento distinto por rol dentro del mismo servicio

Estos son los que te tengo que avisar antes de implementar, porque **no se
pueden resolver con una sola regla por path** sin separarlos:

**`msvc-entrada` — hoy todos bajo `/api/entradas/**`:**

| Endpoint | Rol que lo exige hoy (`exigirRol`) |
|---|---|
| `POST /api/entradas` | CLIENTE |
| `GET /api/entradas` | CLIENTE |
| `GET /api/entradas/{id}` | CLIENTE |
| `POST /api/entradas/{id}/devolucion` | CLIENTE |
| `PUT /api/entradas/{id}/devolucion` | ADMINISTRADOR |
| `POST /api/entradas/{id}/devolucion/reintentar` | ADMINISTRADOR |
| `POST /api/entradas/eventos/{idEvento}/cancelacion` | ADMINISTRADOR |
| `POST /api/entradas/accesos/validar` | PERSONAL_ACCESO |
| `GET /api/entradas/disponibilidad`, `PUT/DELETE /api/entradas/{id}` | sin check de rol |

Propuesta: `/api/entradas/**` para CLIENTE, `/api/admin/entradas/**` para
ADMINISTRADOR y `/api/accesos/escanear` para PERSONAL_ACCESO. El config.yaml
nombra `POST /api/entradas/escanear`, pero el código tiene
`POST /api/entradas/accesos/validar`: hay que alinearlo.

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
parámetro `rol`.

**`msvc-entrada/pagos` y `registros-pago`:** todos los checks son CLIENTE, se
resuelven con una sola regla `/api/pagos/**`. `registros-pago` no tiene ruta
pública y sigue igual.

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
- No se tocan DTOs, reglas de negocio ni paths de las APIs públicas salvo la
  separación de endpoints por rol pendiente de decisión.

## Diferencias con el proyecto de referencia

| Referencia | TicketYa |
|---|---|
| JJWT + `SECRET` hardcodeado | OAuth2 Resource Server + RS256 por entorno |
| Base MySQL `auth` en el gateway | Sin base; identidad en `msvc-usuarios` |
| `/api/authenticate` en el gateway | `POST /api/usuarios/login` en `msvc-usuarios` |
| `X-User-Role` propagado | `X-User-Role` eliminado |
| Roles `ADMIN/PROFESOR/ALUMNO` | roles del seed de TicketYa, con prefijo `ROLE_` |
| `498` para expirado | 401 con `GatewayErrorResponse` |
| `SecurityWebFilterChain` (si fuera reactivo) | `SecurityFilterChain` (MVC) |
| `spring-cloud-starter-gateway-mvc` | `spring-cloud-starter-gateway-server-webmvc` |

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

- Dependencia del login y la emisión en `msvc-usuarios`, que hoy no tiene password
  (punto abierto del config).
- La regla "acceso solo por gateway" requiere red/bind, no solo código.
- `openspec/changes/gateway-entrega1` no está archivado todavía; cuando se
  archive, la spec `gateway-ruteo` debe quedar reemplazada por esta.
- El test existente `GatewayErrorHandlingTest` asume propagación de
  `X-User-Role`; con este cambio dejará de ser válido. No se modifica en este
  paso (AGENTS.md: no modificar tests para que pasen).
