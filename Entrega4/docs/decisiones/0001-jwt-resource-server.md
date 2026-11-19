# ADR-0001: Seguridad JWT en el gateway con OAuth2 Resource Server y RS256

- Estado: propuesta
- Fecha: 2026-11-19

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
  privada. La privada se entrega por entorno o archivo montado, nunca versionada.
- El gateway valida con la clave pública, configurada por
  `${JWT_JWK_SET_URI}` (JWKS publicado por `msvc-usuarios`, opción recomendada)
  o `${JWT_PUBLIC_KEY_LOCATION}` (archivo montado). La audiencia va en
  `${JWT_AUDIENCE}`.
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

La emisión del token en `msvc-usuarios` (email, password BCrypt, JWKS) queda
fuera de este ADR y requiere su propio change.

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

## Consecuencias

### Mejoras

- Validación centralizada y estándar, con RS256 y rotación por JWKS.
- La identidad que reciben los servicios no puede ser suplantada por el cliente.
- Un único lugar donde se decide el rol por ruta.
- Errores 401/403 consistentes con el contrato existente del gateway.

### Costos y riesgos

- `msvc-usuarios` debe incorporar emisión RS256, password y JWKS; es trabajo
  pendiente con su propio change.
- Si se elige JWKS, hace falta una dependencia nueva en `msvc-usuarios`
  (`spring-security-oauth2-jose`), que debe registrarse aquí antes de usarse.
- Las llamadas Feign entre servicios no llevan JWT: quedan fuera del control.
  Es una limitación conocida y documentada.
- "Acceso solo por gateway" requiere una decisión de red o de bind en el
  despliegue; el código de Spring Security no bloquea puertos directos.
- Hay que separar endpoints por rol dentro de `msvc-entrada` y `msvc-evento`
  para que la matriz por path sea suficiente.
- `/api/artistas/**` está en el gateway pero el controlador real es
  `/api/participantes`; debe alinearse.
- El test existente `GatewayErrorHandlingTest` espera `X-User-Role`; dejará de
  ser válido y debe reemplazarse, no "arreglarse para que pase".

## Referencias

- `openspec/changes/gateway-jwt-e2/`
- `openspec/changes/gateway-entrega1/specs/gateway-ruteo/spec.md`
- `openspec/config.yaml`, sección "Seguridad (Entrega 2)"
- Proyecto de referencia analizado: `micro_integrador3/gateway`
