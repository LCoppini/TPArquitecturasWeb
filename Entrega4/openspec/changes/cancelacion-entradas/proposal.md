# Propuesta: cancelación de entradas ante la cancelación de un evento

## Por qué

Al procesar la cancelación de un evento, `msvc-entrada` invalida las entradas
`VALIDA` y `USADA`, pero deja las reservas `RESERVADA` en ese estado. Una
reserva pendiente podría conservar su lugar y seguir siendo elegible para el
flujo de pago aunque el evento ya no deba vender entradas.

Además, el procesamiento de reintegros debe distinguir pagos cobrados de pagos
pendientes o rechazados. Si el reintegro mock falla, la entrada debe permanecer
en un estado que permita reintentar la operación.

## Qué cambia

Actualizar el procesamiento de cancelación de `msvc-entrada` para:

- pasar reservas pendientes a `CANCELADA` y limpiar `expiraEn`, sin intentar
  reintegrarlas;
- pasar entradas emitidas o usadas a `EN_REINTEGRO`, invalidando así su QR;
- procesar solo pagos `APROBADO` o `REINTEGRO_PENDIENTE`;
- dejar las entradas en `EN_REINTEGRO` si falla el reintegro mock, y pasarlas a
  `DEVUELTA` cuando se complete;
- tolerar una repetición del procesamiento sin volver a reintegrar entradas ya
  devueltas.

El cambio no modifica el estado del evento: `msvc-evento` continúa siendo el
servicio dueño de ese estado. `msvc-entrada` procesa la cancelación que recibe
por su operación existente.

## Capacidades

- Nueva capacidad: `entrada-cancelacion` — transiciones de entradas y pagos al
  procesar la cancelación de un evento.

## Alcance

- Cambios en repositorio y servicios de `msvc-entrada`.
- Pruebas unitarias de pago y prueba de integración con PostgreSQL para los
  estados de las entradas y la disponibilidad.
- Reintegro simulado mediante la configuración existente de la pasarela mock.

## Fuera del alcance

- Envío de notificaciones a compradores. La US-4 lo solicita, pero la épica
  EP-09 excluye notificaciones y EP-14 las ubica en Entrega 2; este punto debe
  resolverse en un cambio coordinado con el servicio de notificaciones.
- Cambiar o cancelar el evento desde `msvc-entrada`.
- Integrar una pasarela de pago real o garantizar idempotencia ante un
  proveedor externo. Antes de esa integración, el proveedor deberá aceptar una
  clave de idempotencia para los reintegros.
- Procesamiento por lotes de cancelaciones masivas.

## Impacto

La cancelación cambia los estados persistidos de entradas y pagos, sin cambios
en las rutas HTTP ni en los DTOs existentes. Los estados esperados quedan así:

| Estado previo | Resultado al cancelar | Efecto |
|---|---|---|
| `RESERVADA` | `CANCELADA` | Libera el lugar; no genera reintegro |
| `VALIDA` / `USADA` | `EN_REINTEGRO` | Invalida el QR y permite iniciar reintegro |
| Pago `APROBADO` | `REEMBOLSADO` si el mock responde correctamente | Entrada pasa a `DEVUELTA` |
| Pago `APROBADO` | `REINTEGRO_PENDIENTE` si el mock falla | Entrada permanece `EN_REINTEGRO` |
