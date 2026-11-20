# Tareas

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
      tests HTTP de ese recurso; el controller usa `/api/participantes` y debe
      alinearse en la implementación.
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
- [ ] Agregar `permitAll` para `GET /api/eventos/**` antes de
      `GatewayRoleMatrix.apply(...)` y evitar que la matriz vuelva a exigir roles
      sobre ese prefijo.
- [ ] Agregar `permitAll` para `POST /api/usuarios/registro` y
      `GET /api/artistas/**` antes de `GatewayRoleMatrix.apply(...)`.
- [ ] Agregar la ruta `/api/accesos/**` → `msvc-entrada` en el gateway y en la
      configuración central.
- [ ] Agregar `ADMINISTRADOR` para `/api/admin/artistas/**` y
      `/api/admin/usuarios/**`, con sus rutas hacia `msvc-evento` y
      `msvc-usuarios`.
- [x] Configurar `JwtGrantedAuthoritiesConverter` con claim `roles` y prefijo
      `ROLE_`, y `setPrincipalClaimName("idUsuario")`.
- [x] Crear `IdentityHeadersFilter`: descartar `X-User-Id` entrante, setearlo
      desde `idUsuario`, devolver 401 si el claim falta y eliminar
      `X-User-Role` de `getHeader`, `getHeaders` y `getHeaderNames`.
- [x] Actualizar los `package-info` de `security` y `filter`.

## Servicios de dominio

- [ ] Quitar `X-User-Role` y los helpers `exigirRol` / `exigirRolCliente` de
      `EntradaController`, `PagoController` y `RegistroPagoController`.
- [ ] Quitar el parámetro `rol` de `EntradaClient.cancelarEntradas(...)` y de la
      llamada en `EventoServiceImpl`.
- [ ] Quitar `validarRol` / `tieneRol` por Feign en `EventoServiceImpl`;
      conservar la verificación de pertenencia por `X-User-Id`.
- [ ] Aplicar la separación física de endpoints por rol aprobada.
- [ ] Alinear el controller de participantes con `/api/artistas` (GET) y
      `/api/admin/artistas` (alta/edición); verificado que no hay Feign clients
      ni tests HTTP que usen `/api/participantes`.
- [ ] Implementar `POST /api/usuarios/login` con email + BCrypt y emisión RS256;
      `POST /api/usuarios/registro` público solo `CLIENTE`; roles privilegiados
      por seed o `/api/admin/usuarios`.
- [ ] Exponer `GET /api/eventos/{idEvento}/disponibilidad` en `msvc-evento` y
      delegar en `EntradaClient.consultarDisponibilidad`; mover el contrato
      interno de Entrada a `/internal/entradas/disponibilidad`, sin ruta en el
      gateway.
- [ ] Eliminar `PUT/DELETE /api/entradas/{id}` y sus métodos de service si
      quedan sin uso.
- [ ] Renombrar el escaneo a `POST /api/accesos/escanear` y quitar
      `POST /api/entradas/accesos/validar`.
- [ ] Agregar el esquema `bearerAuth` a la documentación OpenAPI de los
      servicios de dominio.
- [ ] Actualizar los `package-info` de los servicios que describen propagación
      de `X-User-*`.

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
- [ ] Planificar pruebas de 403 por rol insuficiente y matriz completa.
- [ ] Planificar pruebas de login/registro: BCrypt, `CLIENTE` forzado en registro
      y roles privilegiados solo por seed o `/api/admin/usuarios`.
- [ ] Planificar verificación de path único de artistas en gateway, controllers,
      Feign y tests.
- [ ] Planificar pruebas de acceso por rol por endpoint sobre docker-compose.
- [ ] Planificar prueba de carga JMeter con al menos 2 instancias de
      `msvc-entrada` e informe de concurrentes/tiempos/errores.
- [ ] Verificar que `/error` y `/actuator/**` siguen fuera de la cadena de
      seguridad y mantienen el JSON uniforme.
- [x] Validar el gateway actual con Maven 3.9.11/JDK 21 (Maven de IntelliJ por
      ruta completa): `mvn -B -pl msvc-gateway test` → 9 tests, 0 fallos.
      Confirmado que el `HttpServletRequestWrapper` llega al `RestClient` MVC:
      backend recibe `X-User-Id=42` del token, el valor del cliente es pisado y
      `X-User-Role` no se propaga. No hacen falta filtros nativos.

## Documentacion y cierre

- [x] Registrar en el ADR-0001 el reemplazo del test y su motivo.
- [x] Documentar en `design.md` y en el ADR-0001 la diferencia principal con el
      proyecto de referencia: gateway emisor + validador vs gateway solo
      validador Resource Server.
- [x] ADR-0003: contrato del token (RS256, `iss`/`aud`/`iat`/`exp`,
      `idUsuario`, `roles`, 900 s, nombre de claim idéntico en usuarios y
      gateway) y generación de claves PKCS#8 por entorno.
- [ ] Actualizar README con `Authorization: Bearer <token>` y el supuesto de
      acceso solo por gateway.
- [ ] Verificar que no haya secretos ni `.env` versionados; `.env.example` solo
      con placeholders.
- [ ] No archivar el change hasta que los servicios de dominio estén alineados.
