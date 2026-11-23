## ADDED Requirements

### Requirement: Rutas del autoservicio del cliente
#### Scenario: Notificaciones propias
- **WHEN** un CLIENTE llama `GET /api/notificaciones/mias`
- **THEN** el gateway lo enruta a msvc-notificaciones; otros roles reciben 403 y sin token 401

#### Scenario: Ciudad propia
- **WHEN** cualquier usuario autenticado llama `PUT /api/usuarios/me/ciudad`
- **THEN** el gateway lo permite; `PUT /api/usuarios/{id}` sigue siendo de ADMINISTRADOR
