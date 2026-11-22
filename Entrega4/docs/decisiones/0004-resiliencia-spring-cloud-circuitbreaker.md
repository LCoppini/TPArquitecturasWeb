# ADR-0004: Resiliencia con Spring Cloud CircuitBreaker y Resilience4j

- Estado: propuesta (borrador, pendiente de aprobación del grupo)
- Fecha: 2026-11-22

## Contexto

EP-13 requiere timeouts, reintentos, circuit breaker y compensaciones para que
la compra quede consistente cuando fallan servicios externos. Los clients Feign
de `msvc-entrada` y `msvc-evento` hoy no tienen esas políticas. AGENTS.md exige
ADR para toda dependencia nueva. El pago sigue siendo un mock in-process, por lo
que la resiliencia de red aplica solo a los Feign reales.

## Decisión

Usar **Spring Cloud CircuitBreaker con Resilience4j** en `msvc-entrada` y
`msvc-evento`, con esta política:

| Elemento | Valor |
|---|---|
| Feign connect / read | 200 ms / 800 ms |
| TimeLimiter | 1 s |
| Retry | 2 intentos, backoff 100 ms, solo lecturas idempotentes |
| CircuitBreaker | `TIME_BASED`, ventana 120 s, mínimo 3 llamadas, umbral 50 % |
| Abierto | 30 s, half-open con 3 llamadas, transición automática |
| 4xx | no se reintentan ni cuentan como falla |
| `CallNotPermittedException` | ignorada dentro del retry |
| Circuito abierto | 502 en servicios de dominio |

Las compensaciones de la saga se reintentan mediante workers idempotentes y una
restricción `UNIQUE` en BD, no mediante retry de Feign. `EntradaClient` de
cancelación usa solo timeout y circuito; su reintento lo orquesta
`reintentarCoordinacionCancelacion`.

## Alternativas consideradas

- **Implementación manual de reintentos**: descartada; duplica código y no da
  métricas de circuito.
- **Pasarela de pagos real**: descartada; el mock in-process alcanza para este
  trabajo.
- **Retry automático de escrituras**: descartado; puede duplicar cobros o
  cancelaciones.
- **3 intentos de validación**: descartado; 9,9 s de peor caso frente a 6,3 s con
  2 intentos.
- **Idempotencia en memoria**: descartada; no funciona con dos instancias.

## Consecuencias

- Nueva dependencia en los poms de `msvc-entrada` y `msvc-evento`.
- Configuración centralizada en `msvc-config`.
- Conflicto de idempotencia pasa de 422 a 409.
- Aparece el estado `PAGO_APROBADO_SIN_EMITIR` con reemisión y reintegro.
- El borrador debe ser aprobado por el grupo antes de considerarse aceptada.
