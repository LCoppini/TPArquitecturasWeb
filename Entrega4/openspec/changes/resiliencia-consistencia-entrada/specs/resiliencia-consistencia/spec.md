# Requisitos: resiliencia y consistencia de la saga de compra

## ADDED Requirements

### Requirement: Validación previa a la reserva [Entrega 2]

El sistema DEBE validar evento, usuario y sector, en ese orden, antes de crear
una reserva. Si alguna validación falla o agota sus reintentos, el sistema NO
DEBE crear la reserva y DEBE responder un error controlado.

#### Scenario: EventoClient lento

- Dado que `EventoClient` supera el read timeout en todas las llamadas
- Cuando un cliente intenta iniciar una compra
- Entonces se reintentan 2 veces con backoff y se responde 502
- Y **no se reserva ningún lugar** (no se trata de liberar una reserva)

#### Scenario: UsuarioClient caído

- Dado que `msvc-usuarios` no responde
- Cuando se valida el comprador
- Entonces se aplica circuit breaker con fallback y se responde 502
- Y no se crea la reserva

#### Scenario: SectorClient con error de red o 5xx

- Dado que `SectorClient` falla por red o devuelve 5xx
- Cuando se valida el sector
- Entonces se reintenta solo la lectura y se responde 502
- Y no se crea la reserva

#### Scenario: error de negocio 4xx

- Dado que un cliente devuelve 400 o 404
- Cuando se evalúa la respuesta
- Entonces no se reintenta
- Y no cuenta como falla del circuit breaker

### Requirement: Confirmación condicional de la reserva [Entrega 2]

El sistema DEBE confirmar la reserva mediante un `UPDATE` condicional sobre
`estado = 'RESERVADA' AND expiraEn > now()`. Si el `UPDATE` afecta 0 filas, DEBE
reintegrar el pago y NO DEBE emitir la entrada.

#### Scenario: el TTL gana la carrera

- Dado que el pago fue aprobado pero la reserva venció antes de la confirmación
- Cuando se intenta confirmar la reserva
- Entonces el `UPDATE` afecta 0 filas
- Y se solicita el reintegro con la misma clave de idempotencia
- Y no se emite entrada ni se genera código QR

#### Scenario: worker de TTL concurrente con pago aprobado

- Dado que el pago aprobado ya cambió la entrada a `PAGO_APROBADO_SIN_EMITIR`
- Cuando el worker de TTL evalúa esa entrada
- Entonces no la libera, porque su `UPDATE` exige `estado = 'RESERVADA'`
- Y el payment sigue considerado aprobado

#### Scenario: dos instancias confirman la misma reserva

- Dado que dos instancias de `msvc-entrada` intentan confirmar la misma entrada
- Cuando ambas ejecutan el `UPDATE` condicional
- Entonces solo una afecta 1 fila
- Y la otra obtiene 0 filas y no emite

### Requirement: Emisión pendiente tras pago aprobado [Entrega 2]

Después de un pago aprobado, la entrada DEBE pasar a
`PAGO_APROBADO_SIN_EMITIR`. Un worker DEBE retomar ese estado, reemitir hasta 5
intentos con backoff y, si los agota, solicitar el reintegro.

#### Scenario: el worker retoma la emisión

- Dado que una entrada quedó en `PAGO_APROBADO_SIN_EMITIR`
- Cuando el worker la procesa
- Entonces genera el código QR una sola vez
- Y la entrada pasa a `VALIDA` sin duplicar registros

#### Scenario: se agotan los intentos de emisión

- Dado que la emisión falló 5 veces
- Cuando se consume el último intento
- Entonces se solicita el reintegro con la clave de idempotencia del pago
- Y la entrada no queda como `VALIDA` sin QR

### Requirement: Idempotencia de pago [Entrega 1 / modificada en Entrega 2]

La idempotencia DEBE respaldarse con una restricción única en base de datos,
nunca con estado en memoria.

#### Scenario: misma clave y mismo contenido

- Dado que ya existe un pago con esa clave y huella idéntica
- Cuando llega un reintento con la misma clave
- Entonces se devuelve el resultado original
- Y no se realiza un segundo cobro

#### Scenario: misma clave con distinto contenido

- Dado que ya existe un pago con esa clave pero otra huella
- Cuando llega la nueva solicitud
- Entonces responde 409
- Y no se modifica el pago existente

> Nota: este código reemplaza el 422 vigente en `PagoService`.

#### Scenario: dos requests concurrentes con la misma clave

- Dado que dos hilos envían simultáneamente la misma clave y contenido
- Cuando intentan persistir el pago
- Entonces el `UNIQUE (id_entrada, clave_idempotencia)` admite una sola fila
- Y ambas respuestas convergen al mismo resultado
- Y no hay doble cobro

#### Scenario: nuevo intento de compra

- Dado que un pago anterior terminó en `FALLA`
- Cuando el cliente inicia una compra nueva con otra clave
- Entonces se crea otra reserva
- Y se evalúa un nuevo pago sin reutilizar el resultado anterior

### Requirement: Vencimiento de reserva por TTL [Entrega 2]

El sistema DEBE vencer las reservas con más de 10 minutos mediante un `UPDATE`
condicional, de forma idempotente y válida con dos instancias de `msvc-entrada`.

#### Scenario: expiración y liberación

- Dado que una reserva tiene `expiraEn` anterior a `now()`
- Cuando corre el worker de TTL
- Entonces pasa a `VENCIDA` y libera la disponibilidad
- Y una segunda ejecución no produce cambios adicionales

#### Scenario: dos instancias ejecutan el worker

- Dado que dos instancias barren reservas vencidas al mismo tiempo
- Cuando ambas ejecutan el `UPDATE` condicional
- Entonces cada fila es procesada una sola vez
- Y ninguna reserva confirmada por pago es liberada

### Requirement: Resiliencia de clients Feign [Entrega 2]

El sistema DEBE aplicar timeouts, retry limitado a lecturas idempotentes y
circuit breaker con fallback en `EventoClient`, `UsuarioClient`, `SectorClient` y
`EntradaClient`. Con circuito abierto, los servicios de dominio DEBEN responder
**502**, con el mismo contrato que ya usa el reporte.

#### Scenario: circuito abierto

- Dado que el circuito de una dependencia está abierto
- Cuando llega una nueva solicitud
- Entonces se responde 502 por fallback inmediato
- Y `CallNotPermittedException` se ignora dentro del retry

#### Scenario: recuperación half-open a closed

- Dado que el circuito pasó a half-open
- Cuando las llamadas permitidas terminan correctamente
- Entonces el circuito vuelve a closed
- Y se reanuda el flujo normal

#### Scenario: cancelación sin retry automático

- Dado que `EntradaClient.cancelarEntradas` falla por timeout o circuito abierto
- Cuando `EventoServiceImpl` coordina la cancelación
- Entonces no hay retry automático de Feign
- Y el reintento lo realiza `reintentarCoordinacionCancelacion`

### Requirement: Fallas asíncronas aisladas [Entrega 2]

El sistema DEBE mantener la compra confirmada aunque falle una notificación o el
registro de preferencias.

#### Scenario: notificación caída

- Dado que `msvc-notificaciones` no responde
- Cuando se confirma una compra
- Entonces el pago queda `APROBADO` y la entrada `VALIDA`
- Y la falla de aviso no se propaga al flujo de compra

#### Scenario: preferencias caídas

- Dado que el registro de preferencias falla
- Cuando se completa una compra
- Entonces la compra igual queda confirmada
- Y el reintento de preferencias continúa por su outbox de EP-17

### Requirement: Latencia acotada de la validación [Entrega 2]

La validación secuencial de evento, usuario y sector NO DEBE superar 6,3 s en el
peor caso con 2 intentos y timeouts configurados.

#### Scenario: medición del peor caso

- Dado que las tres validaciones agotan 2 intentos con connect 200 ms y read 800 ms
- Cuando se mide el intento de compra
- Entonces la latencia es 3 × (2 × 1,0 s + 0,1 s) = 6,3 s
- Y se prefiere ese valor frente a los 9,9 s que producirían 3 intentos
