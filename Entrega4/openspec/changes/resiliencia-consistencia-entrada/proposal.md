# Propuesta: resiliencia y consistencia de la saga de compra

## Por qué

EP-13 exige que la compra quede consistente cuando falla una dependencia. Hoy los
clients Feign no tienen timeouts, reintentos ni circuit breaker, la compensación
de la saga no está documentada como contrato verificable, y existen dos carreras
no resueltas: el vencimiento de la reserva contra un pago aprobado, y un pago
aprobado cuya emisión no se completó. La idempotencia tampoco está respaldada por
una restricción única en BD, condición necesaria para funcionar con dos
instancias de `msvc-entrada`.

## Qué cambia

- Fijar el orden de la saga: validar evento, usuario y sector **antes** de
  reservar. Con fail-closed, si falla la validación **no se reserva nada**; no se
  trata como “liberar una reserva” porque todavía no existe.
- Confirmar la reserva con un `UPDATE` condicional
  (`estado = 'RESERVADA' AND expiraEn > now()`). Si afecta 0 filas, se reintegra
  el pago y no se emite la entrada. El worker de TTL nunca libera una reserva
  cuyo pago fue aprobado.
- Introducir el estado explícito `PAGO_APROBADO_SIN_EMITIR`, retomado por un
  worker. Política elegida: **reemitir hasta 5 intentos con backoff y, si los
  agota, reintegrar**. Así no queda un cobro sin entrada.
- Respaldar la idempotencia con `UNIQUE` en BD, nunca con estado en memoria.
  Misma clave y mismo contenido devuelve el resultado original sin cobrar de
  nuevo; misma clave con distinto contenido responde **409**; dos requests
  concurrentes con la misma clave producen un solo cobro. Un nuevo intento usa
  otra clave y otra reserva.
- TTL de reserva de 10 minutos configurable, con liberación idempotente y
  segura con dos instancias.
- Resiliencia en los Feign de entrada hacia evento, usuarios y sector, y en
  `EntradaClient` de evento. Para reporte aplica timeout y circuit breaker; para
  cancelación solo timeout y circuit breaker, sin retry automático de Feign
  porque el reintento lo hace `reintentarCoordinacionCancelacion`.
- Errores de negocio 4xx no se reintentan ni cuentan como falla del circuito.
  `CallNotPermittedException` se ignora dentro del retry. Con circuito abierto,
  los servicios de dominio responden **502**, igual que el contrato vigente de
  reportes.
- Documentar la latencia en el peor caso de la validación secuencial y elegir
  **2 intentos** en lugar de 3.
- Especificar que una falla de notificación o de registro de preferencias es
  asíncrona y aislada: la compra igual queda confirmada.

## Capabilities

### Agregada

- `resiliencia-consistencia`: requisitos de saga, compensaciones, idempotencia,
  TTL, resiliencia de Feign y fallas aisladas.

### Modificada

- `reportes-organizador`: solo se agrega resiliencia al Feign
  `EntradaClient.consultarReporte`. No cambia el cálculo, los datos expuestos ni
  las rutas. Se declara explícitamente porque la capability ya está archivada y
  la regla de `openspec/config.yaml` lo exige.
- Compra de Entrega 1: esta capacidad no tiene spec viva publicada, pero el
  change modifica su comportamiento. Cambia la confirmación de reserva, el estado
  posterior al pago y el código de conflicto de idempotencia de 422 a 409. Se
  declara aquí para mantener la trazabilidad del cambio.

## Prerrequisito verificado

- `IdentityHeadersFilter` ya elimina `X-User-Role` y deriva `X-User-Id` del JWT.
- `EntradaClient` de `msvc-evento` no recibe ni envía parámetro `rol`; solo envía
  `X-User-Id` en la cancelación. Tampoco aparece `X-User-Role` en los clients de
  `msvc-entrada`. Queda registrado como prerrequisito ya cerrado.

## Fuera de alcance

- Pasarela de pagos real: el pago sigue siendo mock **in-process** en
  `PagoService`, por decisión del grupo. No se crea `PasarelaClient` ni se aplica
  Resilience4j sobre el mock.
- Implementación de `msvc-notificaciones` y `msvc-preferencias`, incluidas sus
  referencias y su resiliencia interna.
- Caché, trazabilidad/correlationId, log centralizado, Dockerfiles y JMeter.
- Rutas nuevas en el gateway, propagación de `X-User-Role`.
- Modificar tests existentes, en particular `NoDobleVentaTest` y
  `QrAccesoConcurrenciaTest`.
- ADR definitivo: ADR-0004 se entrega en borrador, pendiente de aprobación del
  grupo.
