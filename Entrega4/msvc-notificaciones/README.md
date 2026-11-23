# msvc-notificaciones

Envía emails reales por SMTP (p. ej. Mailtrap) ante estos hechos, de forma asíncrona y con historial en PostgreSQL:

| Tipo | Quién lo solicita | Datos obligatorios |
|---|---|---|
| `COMPRA_CONFIRMADA` | msvc-entrada (`EntradaEmitida`) | `evento`, `fechaHora`, `idEntrada` |
| `DEVOLUCION_APROBADA` | msvc-entrada (`DevolucionAprobada`) | `evento`, `idEntrada`, `monto` |
| `EVENTO_CANCELADO` | msvc-evento | `evento`, `fechaHora` |
| `EVENTO_MODIFICADO` | msvc-evento | `evento`, `cambios` |

El dato opcional `nombre` personaliza el saludo. Specs: `openspec/changes/notificaciones-email`.

**Fuera de este change:** "artista favorito publica un evento en tu ciudad" (depende de `GET /api/preferencias/artistas/{idArtista}/clientes?ciudad=`, trabajo futuro de preferencias) y la invocación desde msvc-entrada / msvc-evento (cambios separados).

## Datos

- Puerto: `8085`.
- Base: PostgreSQL `ticketya_notificaciones`, tabla `notificacion` (historial de envíos). Sin foreign keys hacia otros servicios.

## Contrato interno

Sin ruta en el gateway (`SecurityConfig` deniega `/internal/**`): lo invocan otros microservicios por Eureka (`msvc-notificaciones`).

| Método y ruta | Respuesta |
|---|---|
| `POST /internal/notificaciones` | `202` con la notificación (`PENDIENTE`, o la ya existente si repite la `claveIdempotencia`). `400` si faltan datos |
| `GET /internal/notificaciones/{id}` | `200` / `404` |
| `GET /internal/notificaciones?idUsuario=&estado=` | `200`, hasta 100, la más reciente primero |

```json
POST /internal/notificaciones
{
  "claveIdempotencia": "COMPRA_CONFIRMADA:idEntrada:101",
  "idUsuario": 42,
  "tipo": "COMPRA_CONFIRMADA",
  "destinatario": "cliente@correo.com",
  "datos": { "evento": "Ritual en vivo", "fechaHora": "24/10 21:00", "idEntrada": "101", "nombre": "Ana" }
}
```

`destinatario` es opcional: si falta, se consulta el email a msvc-usuarios. **Hoy `UsuarioResponse` de msvc-usuarios no expone `email`**, así que mientras tanto hay que enviarlo en la solicitud; sin email, la notificación queda `DESCARTADA` (motivo en `ultimoError`).

## Cómo funciona

1. `POST` guarda la notificación como `PENDIENTE` y responde `202` sin esperar al SMTP.
2. Un método `@Async` en un pool propio de hilos (`notif-*`) la reclama de forma atómica (`UPDATE` condicional → `ENVIANDO`) y la envía.
3. Resultado: `ENVIADA`, `FALLIDA` (se reintenta con backoff `10s, 30s, 1m, 5m`, hasta 5 intentos), `PENDIENTE` (circuito abierto: no consume intento) o `DESCARTADA` (usuario inexistente o sin email).
4. Un `@Scheduled` (`poll-ms`) retoma reintentos vencidos y notificaciones `ENVIANDO` trabadas más de 5 minutos: sirve con varias instancias y si el servicio se reinicia.
5. El envío usa Circuit Breaker + TimeLimiter de Resilience4j (instancia `email`); el `correlationId` (`X-Correlation-Id`) se guarda en la notificación y viaja al hilo asíncrono y a los reintentos.

Parámetros en `msvc-config/.../msvc-notificaciones.yml` (`ticketya.notificaciones.*`, `resilience4j.*`).

## Ejecución local

1. Desde la raíz de `Entrega4`: `docker compose up -d`.
2. **Si el volumen de PostgreSQL ya existía**, la base nueva no se crea sola. Crearla una vez:
   `docker exec ticketya-postgres psql -U ticketya -c "CREATE DATABASE ticketya_notificaciones;"`
3. Credenciales SMTP por variables de entorno (por ejemplo, un inbox de [Mailtrap](https://mailtrap.io) → *Email Testing → SMTP Settings*):

   | Variable | Valor por defecto |
   |---|---|
   | `MAIL_HOST` | `sandbox.smtp.mailtrap.io` |
   | `MAIL_PORT` | `2525` |
   | `MAIL_USER` / `MAIL_PASSWORD` | (vacías: el envío falla hasta configurarlas) |
   | `MAIL_FROM` | `no-reply@ticketya.local` |

   En IntelliJ: *Run → Edit Configurations → Environment variables*. Sin credenciales el servicio arranca igual y las notificaciones quedan `FALLIDA` con el motivo en `ultimoError`.
4. Iniciar `msvc-config` y `msvc-eureka` (recompilar `msvc-config` si cambió su YAML) y luego este servicio (`mvn -pl msvc-notificaciones spring-boot:run`).
5. Swagger UI: `http://localhost:8085/swagger-ui.html`.
6. Probar: `POST /internal/notificaciones` (ver ejemplo) y mirar el email en Mailtrap; el estado se consulta con `GET /internal/notificaciones/{id}`.

Si PostgreSQL informa `invalid value for parameter "TimeZone"`, agregar `-Duser.timezone=UTC` a las opciones de VM (ver README de la raíz, punto 7).

## Tests

`mvn -pl msvc-notificaciones test`: unitarios con Mockito y MockMvc (idempotencia, envío, reintentos y backoff, circuito abierto, destinatario, plantillas, controller, correlationId). No requieren base ni SMTP.

## Puntos abiertos

- Exponer `email` en `UsuarioResponse` de msvc-usuarios (cambio aditivo, fuera de este change).
- Invocar este contrato desde msvc-entrada y msvc-evento (cambios separados, ver `cancelacion-entradas`).
- `EventoModificado` no figura aún en `docs/eventos.md`.
