## ADDED Requirements

### Requirement: Detalle de evento para el cliente
`GET /api/eventos/{id}/detalle` devuelve el evento con su lugar y sus sectores, sin requerir autenticación, para cualquier estado del evento.

#### Scenario: Evento con sectores
- **WHEN** se consulta el detalle de un evento existente
- **THEN** la respuesta incluye nombre, dirección y ciudad del lugar
- **AND** cada sector trae idSector, nombre, tipo, capacidad, filas, asientosPorFila (solo numerados) y precio, ordenados por idSector

#### Scenario: Evento no publicado
- **WHEN** el evento está AGOTADO, SUSPENDIDO, CANCELADO o FINALIZADO
- **THEN** el detalle se devuelve con su estado

#### Scenario: Sector desconocido para ubicación
- **WHEN** ubicación no devuelve un sector del evento
- **THEN** el sector conserva idSector y precio, con los demás datos nulos

#### Scenario: Evento inexistente
- **WHEN** el id no existe
- **THEN** responde 404

#### Scenario: Ubicación no disponible
- **WHEN** msvc-ubicacion falla
- **THEN** responde con el error de servicio externo
