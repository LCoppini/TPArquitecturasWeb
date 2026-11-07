# Requisitos: cancelación de entradas

## ADDED Requirements

### Requirement: Cancelación de reservas no pagadas [Entrega 1]

Cuando se procese la cancelación de un evento que tenga una entrada en estado
`RESERVADA`, el sistema DEBE cambiar esa entrada a `CANCELADA` y limpiar su
fecha de expiración.

#### Scenario: una reserva pendiente se cancela

- Dado que existe una entrada `RESERVADA` para el evento cancelado
- Cuando `msvc-entrada` procesa la cancelación
- Entonces la entrada queda en estado `CANCELADA`
- Y su fecha de expiración queda vacía
- Y no se solicita un reintegro para esa entrada

### Requirement: Rechazo de pago de una reserva cancelada [Entrega 1]

Cuando un cliente intente pagar una entrada `CANCELADA`, el sistema DEBE
rechazar la operación y no crear un pago.

#### Scenario: intento de pago posterior a la cancelación

- Dado que la entrada está en estado `CANCELADA`
- Cuando se solicita pagar esa entrada
- Entonces la operación se rechaza como una reserva no disponible
- Y no se crea un pago

### Requirement: Invalidación de entradas emitidas [Entrega 1]

Cuando se procese la cancelación de un evento que tenga entradas en estado
`VALIDA` o `USADA`, el sistema DEBE cambiarlas a `EN_REINTEGRO`.

#### Scenario: entradas emitidas dejan de ser válidas

- Dado que existen entradas `VALIDA` y `USADA` para el evento cancelado
- Cuando `msvc-entrada` procesa la cancelación
- Entonces ambas entradas quedan en estado `EN_REINTEGRO`
- Y sus códigos QR no son aceptados para validar el acceso

### Requirement: Reintegro de pagos cobrados [Entrega 1]

Cuando una entrada `EN_REINTEGRO` tenga un pago `APROBADO` o
`REINTEGRO_PENDIENTE`, el sistema DEBE intentar procesar el reintegro mock.

#### Scenario: el reintegro mock se completa

- Dado que la entrada está `EN_REINTEGRO` y su pago está `APROBADO`
- Cuando el mock de reintegro responde correctamente
- Entonces el pago queda `REEMBOLSADO` con una referencia de reintegro
- Y la entrada queda `DEVUELTA`

#### Scenario: el reintegro mock falla

- Dado que la entrada está `EN_REINTEGRO` y su pago está `APROBADO`
- Cuando el mock de reintegro falla
- Entonces el pago queda `REINTEGRO_PENDIENTE`
- Y la entrada permanece `EN_REINTEGRO` para permitir un reintento

#### Scenario: un pago no cobrado no genera reintegro

- Dado que el pago de una entrada está `PENDIENTE` o `RECHAZADO`
- Cuando se procesa la cancelación del evento
- Entonces el pago no se cambia a `REEMBOLSADO`
- Y la entrada no pasa a `DEVUELTA` por ese pago

### Requirement: Repetición segura del procesamiento [Entrega 1]

Cuando se procese nuevamente la cancelación de un evento cuyas entradas ya
fueron `DEVUELTA`, el sistema DEBE ignorarlas y no generar otro reintegro mock.

#### Scenario: reintento después de un reintegro completado

- Dado que una entrada está `DEVUELTA` y su pago está `REEMBOLSADO`
- Cuando se procesa nuevamente la cancelación del evento
- Entonces no se vuelve a crear un reintegro para esa entrada
- Y la entrada permanece `DEVUELTA`

### Requirement: Notificaciones de cancelación [Entrega 2]

Cuando se cancele un evento, el sistema de notificaciones DEBE informar a los
compradores afectados, sin bloquear el procesamiento de entradas y reintegros.

#### Scenario: notificación desacoplada

- Dado que existen compradores afectados por la cancelación
- Cuando se publique el evento de cancelación para notificaciones
- Entonces cada comprador afectado recibe el aviso correspondiente
- Y una falla del servicio de notificaciones no revierte la cancelación ni los
  estados de entradas y pagos

> Este requisito se documenta como pendiente de Entrega 2 y queda fuera del
> cambio actual `cancelacion-entradas`, conforme a EP-14.
