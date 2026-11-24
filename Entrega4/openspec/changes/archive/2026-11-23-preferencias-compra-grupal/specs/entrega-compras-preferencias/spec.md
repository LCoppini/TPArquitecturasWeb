# Spec Delta

## ADDED Requirements

### Requirement: Persistencia del pendiente en la compra agrupada [Entrega 2]

Cuando una Compra agrupada pasa a PAGADA por `POST /api/compras/{idCompra}/pago`, el sistema MUST (DEBE) registrar un aviso pendiente por cada entrada emitida, en la misma transacción que la entrada y el pago, asignando como titular al usuario que realizó la compra.

#### Scenario: pago aprobado de una compra agrupada
- **WHEN** se aprueba una compra con varias entradas mediante `POST /api/compras/{idCompra}/pago`
- **THEN** queda un aviso pendiente por cada entrada, con `idUsuario` igual al de la compra
- **AND** la respuesta de pago se devuelve sin esperar a `msvc-preferencias`

#### Scenario: pago rechazado o reserva vencida
- **WHEN** el pago de la compra agrupada es rechazado o la reserva vence
- **THEN** no se crea ningún aviso pendiente

#### Scenario: reintento idempotente
- **WHEN** el mismo pago se confirma dos veces con la misma clave de idempotencia
- **THEN** no se duplican los avisos ya registrados para esas entradas

#### Scenario: titular de la preferencia
- **WHEN** la compra agrupada incluye entradas para varios participantes
- **THEN** todos los avisos quedan asociados al usuario que realizó la compra
- **AND** no se crean avisos para los participantes invitados

#### Scenario: trazabilidad
- **WHEN** se registran los avisos durante el pago
- **THEN** conservan el identificador de correlación de la solicitud de pago
