# Design

## Context

Ver `proposal.md` para la motivación. Estado que condiciona este diseño:

- `msvc-notificaciones` (8085, PostgreSQL) es un esqueleto: `pom.xml` con `spring-boot-starter-mail`, OpenFeign y `spring-cloud-starter-circuitbreaker-resilience4j`, `application.yml`, `OpenApiConfig` y un `package-info.java` por paquete. No hay entidades, endpoints ni lógica.
- El grupo decidió que el historial de envíos va en PostgreSQL, que no se usa Redis y que el envío es asíncrono dentro del servicio con `@Async` y un pool propio de hilos (`openspec/config.yaml`).
- `SecurityConfig` del gateway ya deniega `/internal/**` y el gateway no tiene ruta hacia notificaciones: el servicio solo lo invocan otros microservicios por Eureka.
- `Usuario` ya tiene `email` en la base de `msvc-usuarios`, pero `UsuarioResponse` no lo expone todavía.
- No existe aún un filtro de `correlationId` en el gateway ni en los servicios.
- `recomendaciones-preferencias` dejó `GET /api/preferencias/artistas/{idArtista}/clientes?ciudad=` como trabajo futuro, por lo que la notificación "artista favorito" no puede implementarse todavía.

## Goals / Non-Goals

**Goals:**

- Recibir solicitudes de notificación sin que el llamador espere al SMTP ni se vea afectado por sus fallas.
- No duplicar ni perder avisos ante reintentos del llamador, caídas del servicio o varias instancias.
- Enviar emails reales por SMTP con plantillas por tipo y dejar un historial auditable.
- Mantener el estilo del resto de los servicios: capas, DTOs, mapper, excepciones y configuración centralizada.

**Non-Goals (design-level):**

- No se implementan los llamadores en `msvc-entrada` ni `msvc-evento`.
- No se implementa la notificación de artista favorito ni la consulta a `msvc-preferencias`.
- No se agrega broker (RabbitMQ), Redis ni plantillas HTML.

## Decisions

### 1. Contrato interno REST, sin ruta en el gateway

| Endpoint | Exposición | Respuesta |
|---|---|---|
| `POST /internal/notificaciones` | Feign directo por Eureka, sin gateway | `202` con la notificación (nueva o ya existente) |
| `GET /internal/notificaciones/{id}` | Interno | `200` o `404` |
| `GET /internal/notificaciones?idUsuario=&estado=` | Interno | `200`, hasta 100, más recientes primero |

Siguen el patrón de `/internal/preferencias/compras`: nadie con un JWT de cliente puede falsificar avisos y no se modifica `GatewayRoleMatrix`.

- **Alternativa descartada — `/api/notificaciones` detrás del gateway**: exigiría reglas de rol nuevas y expondría un contrato que solo usan otros servicios.
- **Alternativa descartada — broker (RabbitMQ)**: nueva tecnología con ADR y contenedor; el volumen de avisos no lo justifica.

### 2. Solicitud idempotente por `claveIdempotencia`

El llamador define la clave (por ejemplo `COMPRA_CONFIRMADA:idEntrada:101`). Hay una restricción `UNIQUE` sobre la clave. Si ya existe, se devuelve la notificación existente con `202` y no se envía otra. Si dos solicitudes iguales llegan a la vez, la que pierde la inserción relee la existente.

- Si la misma clave llega con otro contenido se devuelve la original: la clave identifica el hecho, no el texto.
- **Alternativa descartada — idempotencia por (usuario, tipo)**: impediría dos compras del mismo cliente.

### 3. Envío asíncrono con `@Async` y estados persistidos

1. `solicitar` guarda la notificación como `PENDIENTE` (la inserción confirma antes de seguir) y llama a `enviarAsync(id)` en **otro bean**, para que el proxy de `@Async` aplique.
2. `enviarAsync` corre en un `ThreadPoolTaskExecutor` propio (`@EnableAsync`) con un `TaskDecorator` que copia el MDC.
3. Si el pool rechaza la tarea, la notificación queda `PENDIENTE` y la recoge el worker de reintentos.

Estados: `PENDIENTE`, `ENVIANDO`, `ENVIADA`, `FALLIDA` y `DESCARTADA` (no se puede enviar y no se reintenta: usuario inexistente o sin email).

- **Alternativa descartada — `@Async` sin persistencia**: la tarea muere con el proceso y se pierde el aviso.
- **Alternativa descartada — enviar dentro del request**: el llamador esperaría al SMTP y heredaría sus fallas.

### 4. Reclamo atómico y reintentos persistidos

Un `UPDATE ... WHERE id = :id AND (estado IN (PENDIENTE, FALLIDA) AND intentos < :max OR estado = ENVIANDO AND fecha_actualizacion < :limite)` pasa la notificación a `ENVIANDO`. Solo el hilo o la instancia que actualiza una fila envía. Un `@Scheduled` busca las elegibles (`PENDIENTE`/`FALLIDA` con `proximo_intento_en` vencido, y `ENVIANDO` trabadas hace más de 5 minutos) y las envía por `@Async`.

```yaml
ticketya:
  notificaciones:
    remitente: no-reply@ticketya.local
    envio:
      intentos-maximos: 5
      backoff: 10s,30s,1m,5m
      stale-timeout: 5m
      espera-circuito-abierto: 35s   # waitDurationInOpenState (30s) + 5s
      poll-ms: 5000
      lote: 20
```

- Al fallar un intento real: `intentos + 1`, estado `FALLIDA` y `proximo_intento_en = ahora + backoff[intentos - 1]`. Al agotar los intentos, queda `FALLIDA` sin próximo intento y se conserva para reproceso manual.
- **Alternativa descartada — claim con `SELECT ... FOR UPDATE SKIP LOCKED`**: mantiene un bloqueo de base durante el envío.

### 5. Resiliencia sobre el SMTP

`EnviadorEmailSmtp` envía con `JavaMailSender` y `SimpleMailMessage`. `EnviadorEmailResiliente` (el `@Primary`) lo envuelve con el `CircuitBreakerFactory` de Spring Cloud (instancia `email`) y un TimeLimiter de 15s, por encima de los timeouts SMTP (conexión 3s, lectura y escritura 5s).

- Si el circuito está abierto (`CallNotPermittedException`) no cuenta como intento: la notificación vuelve a `PENDIENTE` con `proximo_intento_en = ahora + espera-circuito-abierto`.
- Sin `retry` de Resilience4j: el presupuesto de reintentos lo lleva solo el worker persistido, así los intentos son reales.
- **Alternativa descartada — `@CircuitBreaker` por anotación**: requiere AOP y el starter de Resilience4j de Spring Boot, que no están en el `pom.xml`.

### 6. Destinatario

Si la solicitud trae `destinatario`, se usa. Si no, el envío consulta `GET /api/usuarios/{id}` por Feign (`UsuarioClient`) y toma `email`. Resultado de la consulta:

- email presente: se guarda en la notificación y no se vuelve a consultar;
- `404` o email vacío: `DESCARTADA` (no se reintenta, no hay a quién enviar);
- error o timeout de `msvc-usuarios`: intento fallido, se reintenta.

Hoy `UsuarioResponse` no expone `email`; mientras no lo haga, el llamador debe enviar `destinatario`. Es un cambio aditivo en `msvc-usuarios` que se tramita fuera de este change.

### 7. Plantillas

Texto plano en español por tipo, con marcadores `{clave}` reemplazados por los `datos` de la solicitud (mismo enfoque que el `SimpleMailMessage` con plantilla de la guía de Baeldung recomendada por la cátedra). Datos obligatorios por tipo, validados al recibir la solicitud (`400` si faltan):

| Tipo | Datos obligatorios |
|---|---|
| `COMPRA_CONFIRMADA` | `evento`, `fechaHora`, `idEntrada` |
| `DEVOLUCION_APROBADA` | `evento`, `idEntrada`, `monto` |
| `EVENTO_CANCELADO` | `evento`, `fechaHora` |
| `EVENTO_MODIFICADO` | `evento`, `cambios` |

El dato opcional `nombre` personaliza el saludo.

### 8. Trazabilidad

Un filtro de entrada toma `X-Correlation-Id` (o genera uno si falta) y lo pone en el MDC; se devuelve en la respuesta. El `correlationId` se guarda en la notificación y el envío lo restaura en el MDC, tanto en el hilo `@Async` (por el `TaskDecorator`) como en los reintentos. El patrón de log incluye `%X{correlationId}`.

### 9. Historial

La tabla `notificacion` es el historial: `id_usuario` es una referencia por id, sin foreign key. `datos` se guarda como `jsonb`. Se expone solo por los GET internos.

## Risks / Trade-offs

- **[Sin email en `UsuarioResponse`, el destinatario debe venir en la solicitud]** → Mitigación: la notificación queda `DESCARTADA` con motivo explícito; cambio aditivo propuesto fuera de este change.
- **[`ENVIANDO` trabada si muere la instancia]** → Mitigación: recuperación automática a los 5 minutos.
- **[Envío duplicado si una instancia envía y muere antes de registrar `ENVIADA`]** → Aceptado: SMTP no es transaccional y el estado se escribe tras el envío; el duplicado es posible y poco probable.
- **[Una caída prolongada del SMTP mantiene reintentando]** → Mitigación: los intentos reales se agotan a los 5; el rechazo por circuito abierto no los consume.
- **[Pool de hilos saturado]** → Mitigación: la notificación queda `PENDIENTE` y la recoge el worker.
- **[Credenciales SMTP]** → Se leen de variables de entorno (`MAIL_USER`, `MAIL_PASSWORD`); no se versionan.

## Migration Plan

1. No hay migración de datos: la tabla `notificacion` es nueva y se crea con `ddl-auto: update` en desarrollo, igual que el resto del proyecto.
2. Hay que tener la base `ticketya_notificaciones` creada (ver README del servicio) y credenciales SMTP en variables de entorno (por ejemplo, un inbox de Mailtrap).
3. Rollback: dejar de invocar el contrato desde los llamadores. Las notificaciones persistidas pueden reprocesarse o descartarse manualmente.
