## ADDED Requirements

### Requirement: Detalle de evento
#### Scenario: Evento en venta
- **WHEN** se abre /eventos/{id}
- **THEN** se ven sede, participantes y sectores con precio y disponibilidad

#### Scenario: Evento agotado, cancelado, suspendido o finalizado
- **THEN** se muestra su estado y no se ofrece la compra

### Requirement: Compra con fila virtual
#### Scenario: Sin sesión
- **THEN** se invita a ingresar y se vuelve al evento después

#### Scenario: En espera
- **THEN** se muestran el lugar, las personas delante y la espera estimada, con la opción de salir de la fila

#### Scenario: Turno habilitado
- **THEN** se reserva la cantidad elegida con una clave de idempotencia estable y se paga antes del vencimiento

#### Scenario: Resultado del pago
- **THEN** PAGADA lleva a Mis entradas; RECHAZADA y VENCIDA permiten reintentar; en demora se puede actualizar el estado

### Requirement: Mis entradas
#### Scenario: Entrada válida
- **THEN** se muestra su código QR

#### Scenario: Devolución
- **THEN** pide confirmación y muestra el motivo si el backend la rechaza

### Requirement: Sesión vencida
#### Scenario: Token vencido o rechazado
- **THEN** se avisa y se pide ingresar de nuevo
