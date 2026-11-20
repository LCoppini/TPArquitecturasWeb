# ADR-0001: Seguridad JWT en el gateway con OAuth2 Resource Server y RS256

- Estado: aceptada
- Fecha: 2026-11-19
- Actualizado: 2026-11-20

## Contexto

En Entrega 1 el gateway propaga `X-User-Id` y `X-User-Role` sin autenticar. La
épica EP-12 de Entrega 2 exige validar JWT en el gateway, propagar la identidad
derivada del token, aplicar una matriz rol → endpoint y devolver 401/403 con un
formato uniforme.

El `openspec/config.yaml` fija las decisiones del grupo:

- No se crea un `msvc-auth`: el login vive en `msvc-usuarios`
  (`POST /api/usuarios/login`), donde está la entidad `Usuario` con sus roles.
- El token incluye los claims `idUsuario` y `roles`.
- La validación y la autorización ocurren **solo** en `msvc-gateway`, con
  `SecurityFilterChain` de Spring Security en Java.
- Los microservicios no validan el token y reciben la identidad solo por
  `X-User-Id`.
- El gateway es el único punto de entrada para clientes externos.

El equipo también analizó un proyecto de referencia que implementa JWT con
JJWT, una base MySQL `auth` dentro del gateway, un endpoint `/api/authenticate` y
un secreto hardcodeado. Esa solución entra en conflicto con `AGENTS.md`
(prohíbe secretos versionados) y duplica la identidad que ya vive en
`msvc-usuarios`.

## Decision

Adoptar **OAuth2 Resource Server con RS256** en `msvc-gateway`.

- Usar `spring-boot-starter-security` y
  `spring-boot-starter-oauth2-resource-server`. No usar JJWT.
- El par de claves RSA se genera en `msvc-usuarios`, que firma el JWT con la
  privada. La privada se entrega solo por `JWT_PRIVATE_KEY`, nunca versionada.
- El gateway valida con la clave pública configurada por
  `${JWT_PUBLIC_KEY_LOCATION}` mediante `public-key-location`. No se usa JWKS y
  no se agrega `spring-security-oauth2-jose`. La audiencia va en
  `${JWT_AUDIENCE}`.
- `.env.example` documenta las variables; `.env` no se versiona.
- `JwtAuthenticationConverter` mapea el claim `roles` a authorities con prefijo
  `ROLE_` y toma el principal del claim `idUsuario` (`setPrincipalClaimName`).
  **No se usa `sub`.**
- La autorización se expresa con `requestMatchers(...).hasRole(...)` en una
  `SecurityFilterChain` (servlet, porque el gateway es MVC; no
  `SecurityWebFilterChain`). El login es `permitAll`.
- `IdentityHeadersFilter` corre después de Security, descarta cualquier
  `X-User-Id` del cliente, lo reemplaza por `idUsuario`, elimina `X-User-Role` y
  responde 401 si el claim falta.
- Los errores 401 y 403 reutilizan `GatewayErrorResponse`.
- Los servicios de dominio dejan de usar `X-User-Role` y de validar roles por
  Feign; conservan solo la comparación de pertenencia por `X-User-Id`.

La emisión del token en `msvc-usuarios` usa email y password BCrypt, y queda para
su propio change. El registro público crea solo `CLIENTE`; los otros roles se
cargan por seed o por `/api/admin/usuarios` protegido.

## Alternativas consideradas

1. **JJWT + base `auth` en el gateway (proyecto de referencia).** Descartada:
   duplica identidad, agrega JPA/MySQL al gateway y concentra un secreto.
2. **HS256 con secreto compartido.** Descartada por decisión del grupo: RS256
   separa firma y verificación y permite rotación sin compartir la privada.
3. **IdP externo (Keycloak/Auth0).** Descartada: el grupo decidió que el login
   viva en `msvc-usuarios`.
4. **Validar JWT en cada microservicio.** Descartada: duplica validación y
   complica rotación; además el `config.yaml` lo prohíbe explícitamente.
5. **Mantener `X-User-Role` y validar rol en los servicios.** Descartada: el
   cliente podría spoofearlo y contradice "autorización solo en gateway".
6. **Autorización declarativa en YAML.** Descartada: el `config.yaml` pide
   `requestMatchers(...).hasAuthority(...)` en Java.
7. **JWKS publicado por `msvc-usuarios`.** Descartado: agrega una dependencia y
   un endpoint público; se prefiere la clave pública por configuración.

## Diferencia principal con el proyecto de referencia

El proyecto de referencia analizado (`micro_integrador3/gateway`) también usa
`SecurityFilterChain` con `HttpSecurity`, `securityMatcher("/api/**")`, CSRF
deshabilitado y `STATELESS`. Por lo tanto **la cadena no es distinta por ser
reactiva ni por el tipo de filtro**. La diferencia real es de
**responsabilidad**:

- En la referencia, el gateway **emite y valida**: tiene `/api/authenticate`,
  `TokenProvider` con JJWT, `SECRET` hardcodeado, `PasswordEncoder`, `JwtFilter`
  y `@EnableMethodSecurity`.
- En TicketYa, el gateway **solo valida**: usa
  `oauth2ResourceServer().jwt()` con RS256, no genera tokens, no tiene
  `PasswordEncoder`, no habilita `httpBasic` y delega la emisión a
  `msvc-usuarios`.

Consecuencias directas de esa decisión:

| Referencia | TicketYa |
|---|---|
| `TokenProvider.createToken(...)` | Emisión en `msvc-usuarios` |
| `SECRET` HS256 en el código | `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY_LOCATION` por entorno |
| `JwtFilter` + `getSubject()` | Filtro estándar de Spring + claim `idUsuario` |
| `hasAuthority(...)` | `hasRole(...)` con `GatewayRoleMatrix` |
| `498` para expirado | 401/403 con `GatewayErrorResponse` |

El detalle completo de la comparación está en `design.md` del change
`gateway-jwt-e2`.

## Consecuencias

### Mejoras

- Validación centralizada y estándar, con RS256 y clave pública por
  configuración.
- La identidad que reciben los servicios no puede ser suplantada por el cliente.
- Un único lugar donde se decide el rol por ruta.
- Errores 401/403 consistentes con el contrato existente del gateway.

### Costos y riesgos

- `msvc-usuarios` debe incorporar emisión RS256 y password BCrypt; es trabajo
  pendiente con su propio change.
- No se requiere `spring-security-oauth2-jose` porque no se publica JWKS.
- Las llamadas Feign entre servicios no llevan JWT: quedan fuera del control.
  Es una limitación conocida y documentada.
- "Acceso solo por gateway" requiere una decisión de red o de bind en el
  despliegue; el código de Spring Security no bloquea puertos directos.
- Hay que separar endpoints por rol dentro de `msvc-entrada` y `msvc-evento`
  para que la matriz por path sea suficiente.
- Se mantiene `/api/artistas/**`: `GET` público y alta/edición en
  `/api/admin/artistas/**` con `ADMINISTRADOR`. “Artista” designa a cualquier
  participante (artista, equipo o disertante) y la entidad conserva `tipo`.
  El controller actual usa `/api/participantes` y debe alinearse.
- El test existente `GatewayErrorHandlingTest` esperaba `X-User-Role`. Fue
  **reemplazado** en un commit separado: el requisito cambió por decisión de
  diseño (el header se elimina), así que la aserción estaba obsoleta. No se
  modificó un test para ocultar un fallo. Se conservaron las aserciones de
  manejo de errores 404/503 y del backend, y se agregaron las de identidad JWT.

## Registro de reemplazo del test

`msvc-gateway/src/test/java/com/ticketya/gateway/GatewayErrorHandlingTest.java`:

- Se reemplazó la aserción `X-User-Role=CLIENTE` por verificaciones de que el
  backend recibe `X-User-Id` desde el claim `idUsuario`, que **no** recibe
  `X-User-Role` aunque el cliente lo envíe y que un `X-User-Id` del cliente es
  sobrescrito por el del token.
- Se agregaron casos para JWT sin `idUsuario` → 401 y sin token en ruta
  protegida → 401, con el formato uniforme de errores.
- Motivo: con ADR-0001 el gateway ya no propaga roles a los servicios; la
  aserción anterior verificaba un contrato que dejó de existir.
- Las aserciones de 404 uniforme, 503 por backend caído, 503 por Eureka sin
  instancias y conservación del error del servicio destino se mantienen.

## Referencias

- `openspec/changes/gateway-jwt-e2/`
- `openspec/changes/gateway-entrega1/specs/gateway-ruteo/spec.md`
- `openspec/config.yaml`, sección "Seguridad (Entrega 2)"
- Proyecto de referencia analizado: `micro_integrador3/gateway`
