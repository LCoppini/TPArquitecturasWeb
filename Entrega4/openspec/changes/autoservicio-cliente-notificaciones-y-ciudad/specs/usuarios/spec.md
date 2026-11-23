## ADDED Requirements

### Requirement: Cambiar la ciudad propia
#### Scenario: Ciudad válida
- **WHEN** el usuario autenticado envía `PUT /api/usuarios/me/ciudad` con una ciudad no vacía
- **THEN** se actualiza solo su ciudad (recortada) y se devuelve su perfil

#### Scenario: Ciudad vacía o demasiado larga
- **THEN** responde 400 sin modificar nada

#### Scenario: Usuario inexistente o inactivo
- **THEN** responde 404
