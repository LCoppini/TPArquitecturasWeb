## ADDED Requirements

### Requirement: Perfil del usuario autenticado
`GET /api/usuarios/me` devuelve el perfil del usuario identificado por `X-User-Id`, incluido su email.

#### Scenario: Usuario activo
- **WHEN** un usuario autenticado consulta `/api/usuarios/me`
- **THEN** recibe su idUsuario, dni, nombre, apellido, ciudad, email y roles

#### Scenario: Usuario inactivo o inexistente
- **WHEN** el usuario del token no existe o fue dado de baja
- **THEN** responde 404

#### Scenario: Acceso por rol en el gateway
- **WHEN** consulta `/me` un CLIENTE, ORGANIZADOR, ADMINISTRADOR o PERSONAL_ACCESO con token válido
- **THEN** el gateway lo permite
- **AND** sin token responde 401

#### Scenario: Otros usuarios
- **WHEN** un CLIENTE consulta `/api/usuarios/{id}`
- **THEN** el gateway responde 403
