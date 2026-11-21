# Tareas

## Alcance de este cierre

Este change queda con **58 tareas**: las dos verificaciones sobre docker-compose
y JMeter se trasladan a `verificacion-e2e-carga` (EP-21) y se declaran en la
propuesta. El denominador del progreso pasa de 60 a 58.

**Estado final: 58/58 tareas.** Cierre verificado el 2026-11-20 con
`mvn -B package` y las pruebas por módulo; las specs vivas quedan en
`openspec/specs/`.

## Revision previa

- [x] Revisar propuesta, diseño y matriz de roles con el grupo.
- [x] Confirmar el algoritmo RS256 y que el emisor es `msvc-usuarios`.
- [x] Confirmar el enfoque: `SecurityFilterChain` con `HttpSecurity` (gateway MVC).
- [x] Aprobar la separación de paths por rol propuesta en `design.md`.
- [x] Aprobar el ADR `0001-jwt-resource-server.md`.
- [x] Aprobar el ADR `0002-estrategia-pruebas-e2.md`.
- [x] Clave pública por configuración (`public-key-location`) con
      `${JWT_PUBLIC_KEY_LOCATION}`; sin JWKS ni dependencia `jose`.
- [x] Clave privada solo por variable de entorno (`JWT_PRIVATE_KEY`); crear
      `.env.example` con valores de ejemplo y sin secretos reales.
- [x] Disponibilidad pública: mover `GET /api/entradas/disponibilidad` a
      `GET /api/eventos/{idEvento}/disponibilidad`, dentro de
      `GET /api/eventos/**` (`permitAll`). Se verificó que solo devuelve
      `idEvento`, `idSector`, `capacidad`, `ocupadas` y `disponibles`, sin datos
      de compradores.
- [x] Eliminar `PUT/DELETE /api/entradas/{id}`: se verificó que ningún servicio
      los invoca; no hace falta el fallback `/internal/**`. Las bajas siguen
      cubiertas por devolución y cancelación de evento.
- [x] Escaneo QR: `POST /api/accesos/escanear` con `PERSONAL_ACCESO`; el gateway
      rutea `/api/accesos/**` a `msvc-entrada`.
- [x] Mantener `/api/artistas/**`: `GET` público y alta/edición en
      `/api/admin/artistas/**` con `ADMINISTRADOR`. “Artista” designa a cualquier
      participante (artista, equipo o disertante) y la entidad conserva `tipo`.
      Verificado: gateway y YAML usan `/api/artistas/**`; no hay Feign clients ni
      tests HTTP de ese recurso; el controller usaba `/api/participantes` y quedó
      alineado en la implementación.
- [x] Login con `email` + password BCrypt en `POST /api/usuarios/login`; registro
      público solo `CLIENTE` y roles privilegiados por seed o
      `/api/admin/usuarios` protegido.

## Dependencias y configuracion

- [x] Agregar `spring-boot-starter-security`,
      `spring-boot-starter-oauth2-resource-server` y `spring-security-test` al
      POM del gateway.
- [x] Quitar `spring-boot-starter-data-jpa` del gateway.
- [x] Reescribir la plantilla JWT de ambos YAML para RS256 con
      `public-key-location`, audiencia e issuer por variables de entorno.
- [x] Documentar que sin las variables JWT el gateway no arranca (fail closed).

## Implementacion del gateway

- [x] Crear `GatewayRoles` con los roles del seed.
- [x] Crear `GatewayRoleMatrix` con `requestMatchers(...).hasRole(...)`,
      incluida la separación aprobada de paths.
- [x] Crear `GatewaySecurityErrorWriter` reutilizando `GatewayErrorResponse`.
- [x] Crear `SecurityConfig` en `gateway/config` con `SecurityFilterChain` (MVC),
      `securityMatcher("/api/**")`, `permitAll` para `POST /api/usuarios/login`,
      CSRF off y `STATELESS`.
- [x] Agregar `permitAll` para `GET /api/eventos/**` antes de
      `GatewayRoleMatrix.apply(...)` y evitar que la matriz vuelva a exigir roles
      sobre ese prefijo.
- [x] Agregar `permitAll` para `POST /api/usuarios/registro` y
      `GET /api/artistas/**` antes de `GatewayRoleMatrix.apply(...)`.
- [x] Agregar la ruta `/api/accesos/**` → `msvc-entrada` en el gateway y en la
      configuración central.
- [x] Agregar `ADMINISTRADOR` para `/api/admin/artistas/**` y
      `/api/admin/usuarios/**`, con sus rutas hacia `msvc-evento` y
      `msvc-usuarios`.
- [x] `denyAll` para `/internal/**` y fallback `anyRequest().hasRole("CLIENTE")`.
- [x] Configurar `JwtGrantedAuthoritiesConverter` con claim `roles` y prefijo
      `ROLE_`, y `setPrincipalClaimName("idUsuario")`.
- [x] Crear `IdentityHeadersFilter`: descartar `X-User-Id` entrante, setearlo
      desde `idUsuario`, devolver 401 si el claim falta y eliminar
      `X-User-Role` de `getHeader`, `getHeaders` y `getHeaderNames`.
- [x] Actualizar los `package-info` de `security` y `filter`.

## Servicios de dominio

- [x] Quitar `X-User-Role` y los helpers `exigirRol` / `exigirRolCliente` de
      `EntradaController`, `PagoController` y `RegistroPagoController`. Los tres
      controllers conservan solo `X-User-Id` (ADR-0001).
- [x] Quitar el parámetro `rol` de `EntradaClient.cancelarEntradas(...)` y de la
      llamada en `EventoServiceImpl`.
- [x] Quitar `validarRol` / `tieneRol` por Feign en `EventoServiceImpl`;
      conservar la verificación de pertenencia por `X-User-Id`.
- [x] Aplicar la separación física de endpoints por rol aprobada:
      `msvc-evento` con `OrganizadorEventoController` y `AdminEventoController`,
      y `msvc-entrada` con `AdminEntradaController` y `AccesoController`.
- [x] Alinear el controller de participantes con `/api/artistas` (GET) y
      `/api/admin/artistas` (alta/edición); verificado que no hay Feign clients
      ni tests HTTP que usen `/api/participantes`. `ParticipanteController` y
      `AdminParticipanteController` cubren los dos prefijos; la documentación de
      Evento y los `package-info` quedaron alineados.
- [x] Implementar `POST /api/usuarios/login` con email + BCrypt y emisión RS256;
      `POST /api/usuarios/registro` público solo `CLIENTE`; roles privilegiados
      por seed o `/api/admin/usuarios` (ADR-0001). El refactor de usuarios
      expone `POST /api/admin/usuarios` con DTO propio que sí acepta roles,
      service con validación de roles y password hasheado, y tests nuevos.
      No se modificaron tests existentes; el registro público sigue sin
      aceptar roles.
- [x] Exponer `GET /api/eventos/{idEvento}/disponibilidad` en `msvc-evento` y
      delegar en `EntradaClient.consultarDisponibilidad`; mover el contrato
      interno de Entrada a `/internal/entradas/disponibilidad`, sin ruta en el
      gateway. El `path` del `@FeignClient` se dejó en `/` para no romper las
      rutas de ventas y cancelación.
- [x] Eliminar `PUT/DELETE /api/entradas/{id}` y los métodos `actualizar` y
      `eliminar` de `EntradaService`, que solo lanzaban `IllegalStateException`.
- [x] Renombrar el escaneo a `POST /api/accesos/escanear` y quitar
      `POST /api/entradas/accesos/validar`.
- [x] Agregar el esquema `bearerAuth` a la documentación OpenAPI de los
      servicios de dominio. Se declaró HTTP Bearer JWT en los seis módulos que ya
      tenían springdoc, sin agregar dependencias ni ADR nuevo.
- [x] Actualizar los `package-info` de los servicios que describen propagación
      de `X-User-*`. No había descripciones de la propagación simulada de E1; se
      reforzó la de gateway/evento y se agregó la de controllers de Entrada.

## Pruebas

- [x] **Reemplazado** `GatewayErrorHandlingTest`: se eliminó la aserción de
      `X-User-Role` porque el requisito cambió por decisión de diseño (ADR-0001).
      No fue un "arreglar el test para que pase". Commit separado con mensaje
      que referencia el ADR.
- [x] Se conservaron intactas las aserciones de 404 uniforme, 503 por backend
      caído, 503 por Eureka sin instancias y conservación del error del backend.
- [x] Nuevas aserciones: JWT válido → backend recibe `X-User-Id` del claim
      `idUsuario`; `X-User-Role` no llega aunque el cliente lo envíe;
      `X-User-Id` del cliente es pisado por el del token; JWT sin `idUsuario`
      → 401; sin token en ruta protegida → 401; ambos con JSON uniforme.
- [x] Pruebas de 403 por rol insuficiente, endpoints públicos y matriz completa.
      `GatewayRoleMatrixTest` falla si aparece una ruta en el YAML sin regla
      explícita en `GatewayRoleMatrix`.
- [x] Reemplazados los tests de `EventoServiceImplTest` que esperaban el 403 por
      rol consultado por Feign. Ahora cubren `X-User-Id` inválido, pertenencia del
      organizador y borrado administrativo. El cambio se documenta por ADR-0001:
      la autorización por rol pasó al gateway, no se relajaron aserciones.
- [x] **Reemplazado** `ConfiguracionCentralizadaTest.publicaRutasDelGateway`:
      dejó de depender de `routes[0]`/`routes[7]` porque ADR-0001 antepone los
      prefijos por rol. Ahora busca cada ruta por `id` y verifica su `uri`,
      incluida `admin-usuarios-route`. Commit separado, sin relajar aserciones.
- [x] Verificar que los tests existentes de `msvc-usuarios` cubren login con
      BCrypt, `CLIENTE` forzado en registro y roles privilegiados por seed o
      `/api/admin/usuarios`; documentar la cobertura en
      `docs/verificacion-seguridad-rutas.md`.
- [x] Planificar verificación de path único de artistas en gateway, controllers,
      Feign y tests. La matriz y los comandos de verificación quedaron en
      `docs/verificacion-seguridad-rutas.md`.
> Trasladadas a `verificacion-e2e-carga` (EP-21): pruebas de acceso por rol sobre
> docker-compose y prueba de carga JMeter con al menos 2 instancias de
> `msvc-entrada`. No cuentan en el progreso de este change.
- [x] Verificar que `/error` y `/actuator/**` siguen fuera de la cadena de
      seguridad: `GatewaySecurityBoundaryTest` comprueba que `/actuator/health`
      responde 200 y `/error` no devuelve 401/403, mientras `/api/**` sí sigue
      exigiendo token.
- [x] Verificar el fail-closed del gateway: `GatewayFailClosedTest` arranca el
      contexto con un `MockEnvironment` sin `JWT_PUBLIC_KEY_LOCATION`,
      `JWT_AUDIENCE` ni `JWT_ISSUER` y exige que falle por la configuración JWT,
      sin caer en la identidad simulada de Entrega 1.
- [x] Validar el gateway actual con Maven 3.9.11/JDK 21 (Maven de IntelliJ por
      ruta completa): `mvn -B -pl msvc-gateway test` → 40 tests, 0 fallos.
      Confirmado que el `HttpServletRequestWrapper` llega al `RestClient` MVC:
      backend recibe `X-User-Id=42` del token, el valor del cliente es pisado y
      `X-User-Role` no se propaga. No hacen falta filtros nativos.
- [x] **Smoke test punta a punta anterior al refactor**: PostgreSQL real en
      Docker, `msvc-usuarios` con par RS256 temporal fuera del repo y gateway
      validador. `POST /api/usuarios/registro` → `POST /api/usuarios/login` →
      backend stub vía gateway: `X-User-Id=2` coincide con el `idUsuario` del
      token, `X-User-Id=99` del cliente es pisado, `X-User-Role=ADMINISTRADOR`
      del cliente no llega al backend y la ruta protegida sin token responde
      401. Par de claves generado solo en `%TEMP%`, sin secretos versionados.

## Documentacion y cierre

- [x] Registrar en el ADR-0001 el reemplazo del test y su motivo.
- [x] Documentar en `design.md` y en el ADR-0001 la diferencia principal con el
      proyecto de referencia: gateway emisor + validador vs gateway solo
      validador Resource Server.
- [x] ADR-0003: contrato del token (RS256, `iss`/`aud`/`iat`/`exp`,
      `idUsuario`, `roles`, 900 s, nombre de claim idéntico en usuarios y
      gateway) y generación de claves PKCS#8 por entorno.
- [x] Actualizar README con `Authorization: Bearer <token>` y el supuesto de
      acceso solo por gateway.
- [x] Verificar que no haya secretos ni `.env` versionados; `.env.example` solo
      con placeholders.
- [x] No archivar el change hasta que los servicios de dominio estén alineados.
      Alineación verificada: se publican las specs vivas y se archiva el change
      con fecha 2026-11-20.
