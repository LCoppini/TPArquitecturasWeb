# Tasks

## 1. Contrato, modelo y consulta de preferencias

- [x] 1.1 Crear `CompraRegistradaRequest` y `ArtistaCompraRequest` en `msvc-preferencias/dto/request` con validación Bean Validation, y verificar que `mvn -pl msvc-preferencias test` compile y pase un test nuevo de campos obligatorios.
- [x] 1.2 Crear `PreferenciaCliente` (`_id = idUsuario`, `ciudad`, `tiposEvento`, `artistas`, `comprasProcesadas`, `eventosComprados`, `actualizadoEn`) y `ArtistaPreferido`, y verificar con un test de mapeo que el id es el usuario, que `eventosComprados` existe y que solo se guardan identificadores, sin entidades de otros servicios.
- [x] 1.3 Crear `PreferenciaClienteRepository` con el `updateOne` atómico condicional por `idEntrada` mediante un pipeline de agregación que agregue o incremente artistas, agregue o incremente tipos y actualice `eventosComprados` en una sola operación, y verificar con tests de repositorio: primera compra con artista nuevo, segunda compra con artista ya existente, dos compras concurrentes con el mismo artista nuevo, reintento con la misma `idEntrada` y `eventosComprados` poblado sin duplicados.
- [x] 1.4 Crear `PreferenciaService`/`impl` y `InternalCompraController` para `POST /internal/preferencias/compras`, y verificar con tests de servicio y MockMvc la primera compra, el reintento con la misma entrada, el artista repetido y el cambio de ciudad.
- [x] 1.5 Crear los DTOs de respuesta y los dos paths de consulta (`GET /api/preferencias/clientes/{idUsuario}` para CLIENTE y `GET /api/admin/preferencias/clientes/{idUsuario}` para ADMINISTRADOR), comparando el path contra `X-User-Id` solo en el de cliente, y verificar respuestas 200/403 sin exponer entidades JPA ni de MongoDB.

## 2. Recomendaciones dentro de `msvc-preferencias`

- [x] 2.1 Crear los clients Feign hacia `msvc-evento` y `msvc-usuarios` y sus DTOs locales, y verificar con un test de contrato que deserializan `tipo`, `participantes` y `ciudad` con los aliases existentes.
- [x] 2.2 Implementar `RecomendacionService`/`impl` con motivos `MISMO_TIPO`/`MISMO_ARTISTA`, top 5 artistas y top 3 tipos por `cantidad DESC` con desempate ascendente, exclusión de eventos pasados y de todo `idEvento` presente en `eventosComprados` (también en el fallback), fallback por ciudad con `desde=<ahora>` y obtención de ciudad vía `usuarioClient` cuando no hay documento, y verificar cada regla con tests unitarios.
- [x] 2.3 Crear los DTOs de salida con `motivo` y los dos paths (`GET /api/recomendaciones/clientes/{idUsuario}` y `GET /api/admin/recomendaciones/clientes/{idUsuario}`) con la misma comparación de pertenencia que preferencias, y verificar con MockMvc 200/403 y la documentación OpenAPI generada.

## 3. Extensión no rompiente de DTOs en `msvc-entrada`

- [x] 3.1 Extender `EventoResponse` con `tipo` y `participantes` y `UsuarioResponse` con `ciudad`, agregando `ParticipanteResponse` y los constructores sobrecargados `EventoResponse(Long, Instant, String)` y `UsuarioResponse(Long)`, y verificar que `PagoServiceTest`, `EntradaServiceTest` y `UsuarioResponseContractTest` siguen pasando sin editarlos.
- [x] 3.2 Agregar un test de contrato nuevo que deserialice un evento con `tipo` y `participantes` y un usuario con `ciudad`, y verificar que el constructor canónico del record conserva esos valores.

## 4. Outbox y worker en `msvc-entrada`

- [x] 4.1 Agregar el starter de Resilience4j, `@EnableAsync`, `@EnableScheduling`, un `ThreadPoolTaskExecutor` propio con `TaskDecorator` que copia el MDC y un interceptor Feign que envía `X-Correlation-Id`, y verificar con un test de contexto y un test de propagación que el hilo asíncrono conserva el `correlationId`.
- [x] 4.2 Crear la entidad y el repositorio de `pendientes_preferencia` con `UNIQUE (id_entrada)`, claim atómico, recuperación de `EN_PROCESO` con más de 5 minutos y actualización condicionada a `WHERE estado = 'EN_PROCESO'`, y verificar con una prueba de integración sobre el entorno levantado con `docker-compose` (ADR-0002, sin Testcontainers) que dos instancias de `msvc-entrada` no reclaman la misma fila y que el registro stale vuelve a elegible.
- [x] 4.3 Insertar el pendiente dentro de `PagoService.crear()` solo cuando el pago pasa a `APROBADO`, con `idEntrada`, `idUsuario`, `idEvento` y `correlation_id`, y verificar con tests de `PagoService` que aprobado crea una fila, rechazado/vencido no crea ninguna y una rollback no deja residuos.
- [x] 4.4 Crear `PreferenciasClient` y el worker en tres fases (claim, llamada `@Async`, resultado transaccional) con 5 intentos reales en total (no 5 reintentos adicionales a un intento inicial), backoff `10s/30s/1m/5m`, manejo de `CallNotPermittedException` sin consumir intento y estado `FALLIDO` conservado, y verificar con tests unitarios cada transición de estado y el `UPDATE` de resultado de una sola transacción.

## 5. Configuración de resiliencia

- [x] 5.1 Agregar en `msvc-config/.../msvc-entrada.yml` el outbox (`intentos-maximos: 5`, `backoff: 10s,30s,1m,5m`, `stale-timeout: 5m`, `poll-ms: 5s`), el CircuitBreaker `TIME_BASED` con ventana 120s, mínimo 3 llamadas, umbral 50%, `waitDurationInOpenState: 30s` y transición automática a semi abierto, el TimeLimiter de 2s y los timeouts Feign `connect: 500` / `read: 1200` sin retry interno, y verificar con un test de binding que los valores cargados son exactamente esos.

## 6. Gateway: rutas administrativas y matriz de roles

- [x] 6.1 Agregar `admin-preferencias-route` y `admin-recomendaciones-route` hacia `lb://msvc-preferencias` en `msvc-gateway.yml`, y verificar que `GatewayRoleMatrixTest.cadaRutaDelGatewayTieneReglaExplicita` pase para ambos archivos de configuración.
- [x] 6.2 Mover `/api/preferencias/**` y `/api/recomendaciones/**` al array `CLIENTE`, incorporar los nuevos prefijos `/api/admin/preferencias/**` y `/api/admin/recomendaciones/**` al array `ADMIN`, retirar `CLIENTE_Y_ADMIN` y actualizar `prefijosConRegla()`, y verificar que la matriz siga cubriendo todas las rutas declaradas.
- [x] 6.3 Agregar casos a `GatewayRoleMatrixTest` para CLIENTE permitido en los paths de cliente, ADMINISTRADOR permitido en los paths administrativos, ORGANIZADOR denegado en ambos, CLIENTE denegado en los administrativos, ADMINISTRADOR denegado en los de cliente y `POST /internal/preferencias/compras` denegado, y verificar que `mvn -pl msvc-gateway test` pasa.

## 7. Verificación de integración y cierre

- [x] 7.1 Ejecutar `mvn clean package` en el reactor completo y verificar que compilan y pasan los tests de los seis módulos sin orden adicional de comandos.
- [x] 7.2 Levantar el entorno con `docker compose up -d` con dos instancias de `msvc-entrada`, aprobar una compra y verificar que un solo worker reclama cada pendiente, que un registro `EN_PROCESO` stale se recupera y que el mismo `correlationId` aparece en el pago, el worker y preferencias.
- [x] 7.3 Verificar el alcance: `git status --short msvc-notificaciones` queda vacío, no se agregó RabbitMQ/Caffeine/Redis ni ningún ADR más allá del `0004`, no existe notificación de devolución hacia preferencias, los tests preexistentes de `msvc-entrada` están sin cambios y `openspec validate "recomendaciones-preferencias"` termina sin errores.

## 8. Documentación (completada durante la planificación)

- [x] 8.1 Crear `docs/decisiones/0004-mongodb-preferencias.md` con `0000-plantilla.md`, justificando MongoDB para `msvc-preferencias` (documento anidado por cliente, lectura por id, sin joins) y registrando las alternativas descartadas.
- [x] 8.2 Actualizar `msvc-preferencias/.../messaging/package-info.java` para dejar constancia de que el mecanismo es REST + outbox (sin broker) y que el aviso fire-and-forget a `msvc-notificaciones` queda fuera de alcance, y actualizar `msvc-preferencias/README.md` con las decisiones ya resueltas.
