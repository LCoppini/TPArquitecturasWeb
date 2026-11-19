# msvc-notificaciones

**Estado: esqueleto (Entrega 2).** Tiene la estructura de capas, el `pom.xml`, la configuración y un `package-info.java` por paquete con lo previsto. Todavía no tiene lógica.

## Responsabilidad

Enviar emails reales (SMTP, p. ej. Mailtrap) ante estos hechos:

- compra confirmada (`EntradaEmitida`) y devolución aprobada (`DevolucionAprobada`), de msvc-entrada;
- evento cancelado o modificado (`EventoCancelado`, `EventoModificado`), de msvc-evento;
- un artista favorito de un cliente publica un evento en su ciudad (`EventoPublicado`): consulta a msvc-preferencias para saber a quién avisar.

Concentra la configuración SMTP, las plantillas de email y Resilience4j (timeout, retry y circuit breaker) sobre el envío. Lo consumen otros servicios en modo fire-and-forget; no tiene ruta pública en el gateway.

## Datos

- Puerto: `8085`.
- Base: **MongoDB** `ticketya_notificaciones` (un documento por aviso, con estado, intentos y `idEventoOrigen` para evitar duplicados). Modelo tomado de `sugerencia.md` del grupo. Sin foreign keys hacia otros servicios.

## Ejecución local

1. Desde la raíz de `Entrega4`: `docker compose up -d` (incluye el contenedor `mongo`).
2. Iniciar `msvc-config` y `msvc-eureka`, y luego este servicio (`mvn -pl msvc-notificaciones spring-boot:run`). No necesita PostgreSQL.
3. Swagger UI: `http://localhost:8085/swagger-ui.html`.

## Puntos abiertos

- `Usuario` no tiene email: definir de dónde sale el destinatario.
- Mecanismo asincrónico (broker o llamada REST asincrónica): lo decide el grupo y se registra en `docs/decisiones`.
- `EventoModificado` no figura aún en `docs/eventos.md`.
