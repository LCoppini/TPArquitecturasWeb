## ADDED Requirements

### Requirement: Artista favorito publica un evento en tu ciudad
#### Scenario: Publicación con clientes interesados
- **WHEN** se publica un evento con artistas y un cliente tiene a alguno como favorito y la misma ciudad que el lugar
- **THEN** recibe ARTISTA_FAVORITO_PUBLICA_EVENTO con artista, evento, fecha y ciudad

#### Scenario: Varios artistas favoritos del mismo cliente
- **THEN** recibe un solo correo para ese evento

#### Scenario: Republicación
- **WHEN** el evento se suspende y se publica de nuevo
- **THEN** no se envía un segundo correo al mismo cliente (idempotencia)

#### Scenario: Preferencias no disponible
- **THEN** la publicación del evento se completa igual y el error queda en el log

#### Scenario: Sin artistas o sin ciudad
- **THEN** no se consulta ni se envía nada
