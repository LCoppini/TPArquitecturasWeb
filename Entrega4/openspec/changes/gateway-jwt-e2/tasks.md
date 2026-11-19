# Tareas

## Revision previa (bloqueante)

- [ ] Revisar propuesta, diseño y matriz de roles con el grupo.
- [ ] Confirmar el algoritmo RS256 y quién genera el par de claves.
- [ ] Decidir entre JWKS publicado por `msvc-usuarios` (Opción A) o clave pública
      montada (Opción B).
- [ ] Aprobar el ADR `0001-jwt-resource-server.md`.
- [ ] Aprobar el ADR `0002-estrategia-pruebas-e2.md`.
- [ ] Decidir la separación física de endpoints por rol en `msvc-entrada` y
      `msvc-evento` (lista en `design.md`).
- [ ] Alinear `/api/artistas/**` con `/api/participantes` y la ruta de escaneo
      de QR (`/api/entradas/accesos/validar` vs `/api/entradas/escanear`).
- [ ] Definir password de `Usuario` para el login (punto abierto del config).

## Dependencias y configuracion

- [x] Agregar `spring-boot-starter-security`,
      `spring-boot-starter-oauth2-resource-server` y `spring-security-test` al
      POM del gateway.
- [x] Quitar `spring-boot-starter-data-jpa` del gateway.
- [ ] Reescribir la plantilla JWT de ambos YAML para RS256: reemplazar
      `jwk-set-uri` por la opción elegida, agregar `audiences` y password/JWKS
      por variables de entorno.
- [ ] Documentar que sin las variables JWT el gateway no arranca (fail closed).

## Implementacion del gateway

- [ ] Crear `GatewayRoles` con los roles del seed.
- [ ] Crear `GatewayRoleMatrix` con `requestMatchers(...).hasRole(...)`.
- [ ] Crear `GatewaySecurityErrorWriter` reutilizando `GatewayErrorResponse`.
- [ ] Crear `SecurityConfig` con `SecurityFilterChain` (MVC),
      `securityMatcher("/api/**")`, `permitAll` para `POST /api/usuarios/login`,
      CSRF off y `STATELESS`.
- [ ] Configurar `JwtGrantedAuthoritiesConverter` con claim `roles` y prefijo
      `ROLE_`, y `setPrincipalClaimName("idUsuario")`.
- [ ] Crear `IdentityHeadersFilter`: descartar `X-User-Id` entrante, setearlo
      desde `idUsuario`, devolver 401 si el claim falta y eliminar
      `X-User-Role` de `getHeader`, `getHeaders` y `getHeaderNames`.
- [ ] Verificar que `/error` y `/actuator/**` siguen fuera de la cadena de
      seguridad y mantienen el JSON uniforme.

## Servicios de dominio

- [ ] Quitar `X-User-Role` y los helpers `exigirRol` / `exigirRolCliente` de
      `EntradaController`, `PagoController` y `RegistroPagoController`.
- [ ] Quitar el parámetro `rol` de `EntradaClient.cancelarEntradas(...)` y de la
      llamada en `EventoServiceImpl`.
- [ ] Quitar `validarRol` / `tieneRol` por Feign en `EventoServiceImpl`;
      conservar la verificación de pertenencia por `X-User-Id`.
- [ ] Agregar el esquema `bearerAuth` a la documentación OpenAPI de los
      servicios de dominio.
- [ ] Actualizar los `package-info` que hoy describen propagación de
      `X-User-*`.

## Pruebas (solo planificacion en este paso)

- [ ] Reemplazar la expectativa de `GatewayErrorHandlingTest` que asume
      propagación de `X-User-Role` (no modificar tests para que pasen).
- [ ] Planificar pruebas de 401, 403, claim faltante, propagación y spoofing.
- [ ] Planificar pruebas de acceso por rol por endpoint sobre docker-compose.
- [ ] Planificar prueba de carga JMeter con al menos 2 instancias de
      `msvc-entrada` e informe de concurrentes/tiempos/errores.
- [ ] NO escribir ni ejecutar tests en este paso, según lo pedido.

## Documentacion y cierre

- [ ] Actualizar README con `Authorization: Bearer <token>` y el supuesto de
      acceso solo por gateway.
- [ ] Verificar que no haya secretos ni `.env` versionados.
- [ ] No ejecutar `apply` hasta que el grupo apruebe la propuesta y los ADR.

> Nota: las tareas marcadas ya están en el working tree de Entrega 1, pero
> siguen sujetas a revisión del grupo.
