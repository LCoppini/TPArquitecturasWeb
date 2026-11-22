# Proposal: Notificaciones por email

## Why

`msvc-notificaciones` hoy es solo un esqueleto sin lógica: no recibe solicitudes ni envía emails, aunque el TPE pide notificar la compra confirmada, la devolución aprobada y los cambios de un evento. Este change implementa el servicio con envío real por SMTP, sin que una falla de correo pueda afectar a quien lo invoca ni se pierdan o dupliquen avisos.

## What Changes

- Nuevo contrato interno `POST /internal/notificaciones`, idempotente por `claveIdempotencia`: persiste la notificación como `PENDIENTE` y responde `202` sin esperar al SMTP.
- Envío asíncrono dentro del servicio: un método `@Async` ejecutado por un `ThreadPoolTaskExecutor` propio (`@EnableAsync`) con un `TaskDecorator` que copia el `correlationId` del MDC al hilo.
- Envío real con `JavaMailSender` (`spring-boot-starter-mail`), con remitente y SMTP configurables por variables de entorno (Mailtrap en desarrollo), y plantillas de texto por tipo de notificación.
- Historial de envíos en PostgreSQL (`ticketya_notificaciones`) con estados `PENDIENTE`, `ENVIANDO`, `ENVIADA` y `FALLIDA`, intentos, próximo intento y último error.
- Reintentos persistidos: 5 intentos reales con backoff configurable (`10s,30s,1m,5m`), reclamo atómico válido para varias instancias y recuperación de notificaciones `ENVIANDO` trabadas.
- Resiliencia sobre el SMTP: CircuitBreaker y TimeLimiter de Resilience4j; un rechazo por circuito abierto no consume intento.
- Resolución del destinatario: usa `destinatario` si viene en la solicitud; si no, consulta el email a `msvc-usuarios` por Feign. Sin email disponible, la notificación queda `FALLIDA` sin reintentos.
- Consulta interna del historial: `GET /internal/notificaciones/{id}` y `GET /internal/notificaciones?idUsuario=&estado=`.
- Interceptor de entrada que toma `X-Correlation-Id` y lo pone en el MDC, y el `correlationId` se guarda en cada notificación para restaurarlo en los reintentos.

## Capabilities

### New Capabilities

- `notificaciones-envio`: solicitud idempotente, plantillas, envío asíncrono por SMTP, reintentos, resiliencia y resolución del destinatario.
- `notificaciones-historial`: consulta interna del historial y del estado de cada notificación.

### Modified Capabilities

- Ninguna. No se modifica ninguna capability archivada de la Entrega 1.

## Impact

- **Código**: solo `msvc-notificaciones` (entidad, repositorio, servicios, controller, DTOs, mapper, excepciones, configuración, cliente Feign y tests) y su configuración centralizada en `msvc-config`.
- **APIs**: rutas nuevas bajo `/internal/notificaciones`, sin ruta en el gateway (`SecurityConfig` ya deniega `/internal/**`); no se toca `GatewayRoleMatrix`.
- **Datos**: tabla nueva `notificacion` en PostgreSQL `ticketya_notificaciones` (ya creada en `docker/postgres/init`). Sin foreign keys hacia otros servicios.
- **Dependencias**: ninguna nueva; `spring-boot-starter-mail`, `spring-cloud-starter-circuitbreaker-resilience4j` y OpenFeign ya están en el `pom.xml` del servicio. No se agregan RabbitMQ, Redis ni Caffeine.
- **Specs de Entrega 1**: no se modifica ninguna capability archivada.

## Fuera del alcance

- No se modifican `msvc-entrada`, `msvc-evento`, `msvc-usuarios` ni `msvc-gateway`: invocar este contrato desde Entrada (`EntradaEmitida`, `DevolucionAprobada`) y Evento (`EventoCancelado`, `EventoModificado`) se tramita en changes separados (ver `cancelacion-entradas`).
- No se implementa la notificación "artista favorito publica un evento en tu ciudad" (`EventoPublicado`): depende de `GET /api/preferencias/artistas/{idArtista}/clientes?ciudad=`, que `recomendaciones-preferencias` dejó como trabajo futuro, y su contrato no fue revisado. El tipo `ARTISTA_FAVORITO_PUBLICA_EVENTO` no se incluye en este change.
- No se expone el email en `UsuarioResponse` de `msvc-usuarios`: hasta que se agregue, el destinatario debe enviarse en la solicitud o la notificación queda `FALLIDA` por falta de destinatario.
- No hay ruta pública ni administrativa en el gateway para el historial.
- No hay plantillas HTML, adjuntos ni internacionalización: solo texto plano en español.
- No se implementa el log centralizado en archivos ni el filtro `X-Correlation-Id` del gateway (transversal): este servicio solo propaga el header que reciba.
- No se requiere ADR nuevo.
