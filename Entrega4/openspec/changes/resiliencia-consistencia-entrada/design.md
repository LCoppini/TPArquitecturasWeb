# Diseño: resiliencia y consistencia de la saga de compra

## Contexto

`msvc-entrada` orquesta reserva, pago y emisión. Hoy los Feign no tienen
timeouts ni circuit breaker, la confirmación de reserva no es condicional, y un
pago aprobado puede quedar sin emisión si el proceso se cae a mitad. El pago es
un mock **in-process** en `PagoService`, no una llamada HTTP/Feign. El alcance de
este change es EP-13: saga, compensaciones y resiliencia de los clients reales.

## Orden de la saga y tabla de compensaciones

| Paso | Falla | Compensación | Idempotente | Reintento seguro |
|---|---|---|---|---|
| 1. Validar evento, usuario y sector | lento o caído | no reservar; responder 502 | no aplica, aún no hay estado | sí, solo retry automático de lecturas |
| 2. Reservar | error al persistir | no continuar; respaldo por TTL | sí, `UNIQUE` y `UPDATE` condicional | sí, worker de TTL |
| 3. Confirmar reserva tras pago | `UPDATE` afecta 0 filas porque el TTL ganó | reintegrar y no emitir | sí, clave de pago única | sí |
| 4. Pagar | **pago rechazado por el mock** | devolver resultado original; liberar reserva | sí, `UNIQUE (id_entrada, clave_idempotencia)` | sí |
| 5. Emitir entrada | falla al generar QR | estado `PAGO_APROBADO_SIN_EMITIR`, worker reintenta 5 veces y luego reintegra | sí, QR único | sí |
| 6. Cancelar evento / reintegrar | mock de reintegro en `FALLA` | `REINTEGRO_PENDIENTE` → `DEVUELTA` | sí | sí, `reintentarCoordinacionCancelacion` |
| 7. Reporte (evento → entrada) | entrada lenta o caída | responder 502 sin cifras | no aplica | timeout y circuit breaker, sin retry |

## Decisiones

- **Validación antes de reservar.** Si falla evento, usuario o sector, no se crea
  la reserva. Corregimos la redacción anterior: no se “libera” una reserva que
  nunca existió.
- **Carrera TTL vs pago.** La confirmación es
  `UPDATE ... WHERE estado = 'RESERVADA' AND expiraEn > now()`. Si afecta 0
  filas, se reintegra y no se emite. El worker de TTL usa la misma condición, por
  lo que nunca libera una reserva ya aprobada.
- **Emisión interrumpida.** Se adopta el estado `PAGO_APROBADO_SIN_EMITIR` y la
  política de **reemitir hasta 5 intentos con backoff 100/200/400/800 ms; si se
  agotan, reintegrar**. Se descartó dejar el cobro pendiente sin entrada porque
  rompe la regla de “no cobro sin entrada”.
- **Idempotencia en BD.** `UNIQUE (id_entrada, clave_idempotencia)`. Misma clave
  e igual huella devuelve el resultado original; distinta huella responde 409
  (antes 422). Dos concurrentes resuelven por la constraint. No se usa memoria.
- **TTL.** 10 minutos configurables. El barrido es condicional e idempotente y
  funciona con dos instancias sin coordination en memoria.
- **Resiliencia.** Spring Cloud CircuitBreaker con Resilience4j. Retry automático
  solo en lecturas idempotentes: 2 intentos, backoff 100 ms. Feign connect
  200 ms, read 800 ms y `TimeLimiter` 1 s, alineados. Circuit breaker
  `TIME_BASED`, ventana 120 s, mínimo 3 llamadas, umbral 50 %, abierto 30 s,
  half-open 3 llamadas y transición automática. Los 4xx no se reintentan ni
  cuentan como falla; `CallNotPermittedException` se ignora en el retry.
- **Código HTTP.** Circuito abierto responde **502** en `msvc-entrada` y
  `msvc-evento`, igual que el contrato ya publicado por el reporte. El gateway
  mantiene su 503 propio para destinos no disponibles; son conceptos distintos y
  se documenta la diferencia.
- **EntradaClient de cancelación.** Solo timeout y circuito, sin retry
  automático de Feign. El reintento lo hace `reintentarCoordinacionCancelacion`,
  porque es una escritura y debe ser orquestado.
- **Latencia.** Con 2 intentos el peor caso de validar evento, usuario y sector
  en secuencia es `3 × (2 × 1,0 s + 0,1 s) = 6,3 s`. Con 3 intentos serían 9,9 s.
  Elegimos 2 intentos.
- **Mock in-process.** No se aplica Resilience4j a `PagoService`; el circuito
  breaker cubre únicamente clients Feign y el worker de emisión.
- **Fallas asíncronas.** Notificaciones y preferencias son fire-and-forget; su
  falla no revierte una compra confirmada.

## Alternativas descartadas

- Pasarela de pagos real: fuera de alcance por decisión del grupo.
- Meter broker en el flujo de compra: la compra sigue siendo síncrona.
- Retry automático de Feign sobre escrituras: puede duplicar efectos.
- 3 intentos de validación: 9,9 s de peor caso, considerado excesivo.
- Idempotencia en memoria: falla con dos instancias.
- Testcontainers nuevos: ADR-0002 exige compose para E2E; los tests existentes
  no se modifican.

## Pruebas

- Misma clave concurrente y carrera TTL/pago: contra la base PostgreSQL ya
  levantada por `docker compose up -d`, con dos hilos en un mismo
  `@SpringBootTest`. Sin Testcontainers nuevos.
- TTL con dos instancias reales: sobre `docker-compose` en EP-21, dentro del
  change `verificacion-e2e-carga`.
- `NoDobleVentaTest` y `QrAccesoConcurrenciaTest` quedan intactos.

## Consecuencias

- Nueva dependencia en `msvc-entrada` y `msvc-evento`: se documenta en ADR-0004,
  en borrador y pendiente de aprobación.
- Configuración central en `msvc-config`.
- Cambio de contrato: conflicto de idempotencia pasa de 422 a 409.
- Nuevo estado `PAGO_APROBADO_SIN_EMITIR`; si la columna `estado` es varchar no
  requiere migración de esquema, pero debe verificarse antes de aplicar.
