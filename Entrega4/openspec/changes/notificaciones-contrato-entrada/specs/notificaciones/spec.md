## ADDED Requirements

### Requirement: Compra confirmada
#### Scenario: Pago aprobado
- **WHEN** una Compra pasa a PAGADA
- **THEN** se solicita un único COMPRA_CONFIRMADA con clave `COMPRA_CONFIRMADA:idCompra:<id>`, evento, fecha e ids de entrada

### Requirement: Evento cancelado
#### Scenario: Cancelación
- **WHEN** se cancela un evento con entradas VALIDA
- **THEN** cada usuario afectado recibe EVENTO_CANCELADO (clave por evento y usuario)

#### Scenario: Falla el envío
- **THEN** la cancelación se completa igual y el error queda en el log
