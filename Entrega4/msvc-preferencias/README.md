# msvc-preferencias

**Estado:** capacidades definidas en el change OpenSpec `recomendaciones-preferencias`; la implementación se sigue en `openspec/changes/recomendaciones-preferencias/tasks.md`.

## Responsabilidad

- Registrar automáticamente las preferencias de cada cliente (tipos de evento, artistas y ciudad) a partir de sus compras aprobadas: cada vez que msvc-entrada emite una entrada, se registra el tipo y los artistas del evento.
- Recomendar eventos del mismo tipo y/o con los mismos artistas favoritos. Versión simplificada, sin "quienes compraron esto también compraron" ni popularidad; la justificación está en `docs/decisiones/0004-mongodb-preferencias.md` y en el `design.md` del change.
- Informar a msvc-notificaciones qué clientes tienen a un artista como favorito: **fuera de alcance** en este change (sin broker y sin aviso fire-and-forget).

## Datos

- Puerto: `8086`.
- Base: **MongoDB 7** `ticketya_preferencias`, un documento por cliente (`_id = idUsuario`), justificada en `ADR-0004`. El paquete se llama `entity` para mantener la misma estructura que los demás servicios.
- El documento guarda `ciudad`, `tiposEvento`, `artistas`, `comprasProcesadas` (idempotencia por `idEntrada`) y `eventosComprados` (para excluir eventos ya comprados de las recomendaciones).

## Ejecución local

1. Desde la raíz de `Entrega4`: `docker compose up -d` (incluye el contenedor `mongo`).
2. Iniciar `msvc-config` y `msvc-eureka`, y luego este servicio (`mvn -pl msvc-preferencias spring-boot:run`). No necesita PostgreSQL.
3. Swagger UI: `http://localhost:8086/swagger-ui.html`.

## Gateway

- Cliente: `GET /api/preferencias/clientes/{idUsuario}` y `GET /api/recomendaciones/clientes/{idUsuario}`. El servicio compara `{idUsuario}` contra `X-User-Id`.
- Administrador: `GET /api/admin/preferencias/clientes/{idUsuario}` y `GET /api/admin/recomendaciones/clientes/{idUsuario}`. El gateway autoriza por rol según el path; el servicio no recibe ni consulta el rol.
- Interno (sin ruta en el gateway): `POST /internal/preferencias/compras`, invocado por Feign desde el worker de msvc-entrada.

## Decisiones resueltas

- MongoDB para el perfil de gustos: `docs/decisiones/0004-mongodb-preferencias.md`.
- Recepción de compras mediante REST interno + outbox persistido en msvc-entrada, sin RabbitMQ ni otros brokers: ver `design.md` del change y `messaging/package-info.java`.
- No se agregan Redis, Caffeine, co-ocurrencia ni popularidad en este change.
