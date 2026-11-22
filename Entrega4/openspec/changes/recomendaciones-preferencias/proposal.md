# Proposal: Recomendaciones y preferencias

## Why

`msvc-preferencias` hoy es solo un esqueleto sin lógica: no registra preferencias ni expone recomendaciones, aunque el proyecto ya prevé que los gustos se deriven automáticamente del historial de compras. Esta entrega finaliza esa capacidad sin crear un microservicio nuevo y sin que una falla de preferencias pueda afectar la compra de entradas.

## What Changes

- Registro automático e idempotente de preferencias por compra aprobada (tipo de evento, artistas participantes y ciudad del cliente), implementado como upsert por `idEntrada`.
- Nuevo contrato interno `POST /internal/preferencias/compras`, invocado por Feign desde un worker de `msvc-entrada`, sin ruta en el gateway.
- Outbox persistido en `msvc-entrada` (`pendientes_preferencia`) con claim atómico válido para dos instancias, 5 intentos reales en total (no 5 reintentos adicionales a un intento inicial), con backoff `10s/30s/1m/5m`, reintento sin consumo de presupuesto cuando el CircuitBreaker está abierto y conservación de registros `FALLIDO` para replay manual.
- Nuevo endpoint público `GET /api/recomendaciones/clientes/{idUsuario}` con motivos `MISMO_TIPO`, `MISMO_ARTISTA` y `FALLBACK_CIUDAD`, basado en top 5 artistas / 3 tipos y fallback por ciudad con `desde=<ahora>`, excluyendo los eventos que el cliente ya compró (EP-17).
- Regla de pertenencia por path: un `CLIENTE` consulta solo su propio `{idUsuario}` en `/api/preferencias/clientes/{idUsuario}` y `/api/recomendaciones/clientes/{idUsuario}`; un `ADMINISTRADOR` consulta cualquier `{idUsuario}` en `/api/admin/preferencias/clientes/{idUsuario}` y `/api/admin/recomendaciones/clientes/{idUsuario}`. El gateway autoriza por rol según el path, sin propagar roles al servicio.
- Extensión no rompiente de los DTOs Feign locales de `msvc-entrada` (`EventoResponse` con `tipo` y `participantes`, `UsuarioResponse` con `ciudad`) mediante constructores sobrecargados que conservan las firmas existentes.
- Configuración de resiliencia sobre la llamada a preferencias: CircuitBreaker `TIME_BASED`, TimeLimiter de 2s y timeouts HTTP por debajo del TimeLimiter, sin retry interno de Resilience4j.
- Propagación del `correlationId` al hilo asíncrono y al header `X-Correlation-Id` del Feign.

## Capabilities

### New Capabilities

- `preferencias-cliente`: documento de preferencias del cliente, registro idempotente a partir de compras y consulta de preferencias.
- `recomendaciones`: selección de eventos sugeridos con motivos y fallback por ciudad ante ausencia de historial o de coincidencias.
- `entrega-compras-preferencias`: entrega confiable de la notificación de compra desde `msvc-entrada`, con outbox, reintentos, circuit breaker y trazabilidad.

### Modified Capabilities

- Ninguna. La pertenencia de `{idUsuario}` contra `X-User-Id` reutiliza el criterio ya vigente de ADR-0001 y queda especificada dentro de `preferencias-cliente` y `recomendaciones`; no se agrega un delta a `identidad-servicios`.

## Impact

- **Código**: `msvc-preferencias` (controllers, service, repository, entidad, DTOs y clients nuevos); `msvc-entrada` (nueva entidad, repository, worker y Feign client, extensión de dos DTOs de respuesta, `@EnableAsync`/`@EnableScheduling` y `TaskDecorator`); `msvc-gateway` (dos rutas administrativas nuevas, ajuste de `GatewayRoleMatrix` y casos de test nuevos).
- **APIs**: nuevas rutas `GET /api/preferencias/clientes/{idUsuario}` y `GET /api/recomendaciones/clientes/{idUsuario}` para `CLIENTE`, y `GET /api/admin/preferencias/clientes/{idUsuario}` y `GET /api/admin/recomendaciones/clientes/{idUsuario}` para `ADMINISTRADOR`; nuevo contrato interno `/internal/preferencias/compras`, sin exposición en gateway.
- **Datos**: MongoDB `ticketya_preferencias` (nueva colección) y PostgreSQL `ticketya_entrada` (nueva tabla `pendientes_preferencia`). Sin migración de datos existentes ni foreign keys entre servicios.
- **Dependencias y decisiones**: `spring-cloud-starter-circuitbreaker-resilience4j` en `msvc-entrada`, ya adoptado en otros módulos del proyecto; no corresponde ADR nuevo para resiliencia. Se crea `ADR-0004` para justificar MongoDB en `msvc-preferencias`, requisito de `AGENTS.md` que estaba pendiente en el README del módulo. No se agregan RabbitMQ, Caffeine ni Redis.
- **Specs de Entrega 1**: no se modifica ninguna capability archivada. Los cambios en `msvc-entrada` son aditivos (campos nuevos en DTOs de respuesta y tabla nueva) y no alteran reserva, pago ni emisión.

## Fuera del alcance

- No se crea el microservicio `msvc-recomendaciones`: la recomendación es una capacidad dentro de `msvc-preferencias`.
- No se implementa `GET /api/preferencias/artistas/{idArtista}/clientes?ciudad=`, que consumirá `msvc-notificaciones`; queda documentado como trabajo futuro. No se modifica `msvc-notificaciones`.
- No se agregan RabbitMQ, el evento `UsuarioActualizado`, co-ocurrencia, popularidad ni cálculo de ventas.
- No se descuentan preferencias por devolución: decisión de alcance, porque no existe notificación de devoluciones hacia preferencias en este change.
- No se modifica `sinFiltros` ni la búsqueda de `msvc-evento`; el cambio de eficiencia en `buscar()` se tramita en un flujo OpenSpec separado.
- No se modifica la lógica de los servicios para reconocer roles: la distinción `CLIENTE`/`ADMINISTRADOR` se resuelve únicamente por path en el gateway, según ADR-0001. No se propaga `X-User-Role` ni se consulta `msvc-usuarios` para reconstruir el rol.
