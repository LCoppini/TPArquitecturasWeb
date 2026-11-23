## ADDED Requirements

### Requirement: Asignación automática de asientos
#### Scenario: Sector numerado sin asiento
- **WHEN** un item de la Compra apunta a un sector NUMERADO sin fila ni asiento
- **THEN** se le asigna el primer asiento libre, saltando ocupados y los elegidos en la misma Compra

#### Scenario: Sin lugares
- **WHEN** no quedan asientos libres
- **THEN** responde 409 y la Compra no se crea

#### Scenario: Sector no numerado o asiento explícito
- **THEN** el comportamiento no cambia

### Requirement: Consulta de la fila del evento
#### Scenario: Evento con fila
- **WHEN** el cliente consulta GET /api/filas-virtuales/eventos/{id}
- **THEN** recibe estado, maxPersonas y cupoEntradas

#### Scenario: Evento sin fila
- **THEN** responde 404

### Requirement: Entrada con su Compra
#### Scenario: Listado del cliente
- **WHEN** el cliente consulta GET /api/entradas
- **THEN** cada entrada incluye idCompra, estadoDevolucion y motivoInvalidacion
