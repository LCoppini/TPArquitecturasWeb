# Tareas

## Revision previa

- [x] Revisar propuesta, diseño y matriz de roles con el grupo.
- [x] Confirmar el algoritmo RS256 y que el emisor es `msvc-usuarios`.
- [x] Confirmar el enfoque: `SecurityFilterChain` con `HttpSecurity` (gateway MVC).
- [x] Aprobar la separación de paths por rol propuesta en `design.md`.
- [x] Aprobar el ADR `0001-jwt-resource-server.md`.
- [x] Aprobar el ADR `0002-estrategia-pruebas-e2.md`.
- [ ] Decidir entre JWKS publicado por `msvc-usuarios` (Opción A) o clave pública
      montada (Opción B). La plantilla usa JWKS por ahora.
- [ ] Alinear `/api/artistas/**` con `/api/participantes` y la ruta de escaneo
      de QR (`/api/entradas/accesos/validar` vs `/api/entradas/escanear`).
- [ ] Definir password de `Usuario` para el login (punto abierto del config).

## Dependencias y configuracion

- [x] Agregar `spring-boot-starter-security`,
      `spring-boot-starter-oauth2-resource-server` y `spring-security-test` al
      POM del gateway.
- [x] Quitar `spring-boot-starter-data-jpa` del gateway.
- [x] Reescribir la plantilla JWT de ambos YAML para RS256: JWKS o clave pública
      y audiencia por variables de entorno.
- [x] Documentar que sin las variables JWT el gateway no arranca (fail closed).

## Implementacion del gateway

- [x] Crear `GatewayRoles` con los roles del seed.
- [x] Crear `GatewayRoleMatrix` con `requestMatchers(...).hasRole(...)`,
      incluida la separación aprobada de paths.
- [x] Crear `GatewaySecurityErrorWriter` reutilizando `GatewayErrorResponse`.
- [x] Crear `SecurityConfig` con `SecurityFilterChain` (MVC),
      `securityMatcher("/api/**")`, `permitAll` para `POST /api/usuarios/login`,
      CSRF off y `STATELESS`.
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
- [ ] Planificar pruebas de acceso por rol por endpoint sobre docker-compose.
- [ ] Planificar prueba de carga JMeter con al menos 2 instancias de
      `msvc-entrada` e informe de concurrentes/tiempos/errores.
- [ ] Verificar que `/error` y `/actuator/**` siguen fuera de la cadena de
      seguridad y mantienen el JSON uniforme.
- [ ] No se ejecutaron pruebas en este paso (Maven no disponible en el PATH).

## Documentacion y cierre

- [x] Registrar en el ADR-0001 el reemplazo del test y su motivo.
- [ ] Actualizar README con `Authorization: Bearer <token>` y el supuesto de
      acceso solo por gateway.
- [ ] Verificar que no haya secretos ni `.env` versionados.
- [ ] No archivar el change hasta que los servicios de dominio estén alineados.
