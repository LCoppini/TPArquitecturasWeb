## ADDED Requirements

### Requirement: Historial propio de notificaciones
#### Scenario: Cliente con notificaciones
- **WHEN** el cliente autenticado consulta `GET /api/notificaciones/mias`
- **THEN** recibe solo sus notificaciones, la más reciente primero, con tipo, asunto, estado y fechas

#### Scenario: Sin datos internos
- **THEN** la respuesta no incluye `ultimoError`, `claveIdempotencia` ni `correlationId`

#### Scenario: Sin identidad
- **WHEN** falta el header `X-User-Id`
- **THEN** la solicitud es inválida (4xx)
