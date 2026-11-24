## ADDED Requirements

### Requirement: Devolución aprobada
#### Scenario: Reintegro completado
- **WHEN** un administrador aprueba la devolución de una Compra y el reintegro se concreta
- **THEN** el cliente recibe un único DEVOLUCION_APROBADA con evento, ids de entrada y monto reintegrado

#### Scenario: Reintegro pendiente
- **WHEN** el reintegro falla y queda pendiente
- **THEN** no se envía el aviso; se envía al completarse con un reintento

#### Scenario: Devolución rechazada o cancelación de evento
- **THEN** no se envía DEVOLUCION_APROBADA

#### Scenario: Falla el envío
- **THEN** la devolución se completa igual y el error queda en el log
