# Spec Delta

## Purpose

Define la gestión del ciclo de vida de los usuarios de TicketYa y la consulta de sus datos por identificador para los servicios que dependen de ellos.

## ADDED Requirements

### Requirement: Alta de usuarios (Entrega 1)
Cuando se recibe una solicitud válida para registrar un usuario, el sistema deberá (SHALL) crear un usuario activo con identificador propio y los datos de perfil solicitados.

#### Scenario: Alta válida
- **WHEN** se envían DNI, nombre, apellido, ciudad y roles permitidos para un usuario nuevo
- **THEN** el sistema crea el usuario activo y responde con código 201 y su representación, incluido el identificador

### Requirement: Consulta de usuarios (Entrega 1)
Cuando se consulta un usuario activo por identificador o se solicita el listado, el sistema deberá (SHALL) devolver los datos de perfil y los roles asociados sin exponer detalles internos de persistencia.

#### Scenario: Consulta por identificador
- **WHEN** un servicio consulta `GET /api/usuarios/{id}` para un usuario activo existente
- **THEN** el sistema responde con código 200 y un objeto que incluye `idUsuario`, DNI, nombre, apellido, ciudad y roles

#### Scenario: Identificador inexistente o dado de baja
- **WHEN** se consulta por identificador un usuario inexistente o inactivo
- **THEN** el sistema responde con código 404

#### Scenario: Listado
- **WHEN** se solicita `GET /api/usuarios`
- **THEN** el sistema devuelve los usuarios activos con sus datos de perfil y roles

### Requirement: Modificación de usuarios (Entrega 1)
Cuando se recibe una solicitud válida para modificar un usuario activo, el sistema deberá (SHALL) actualizar sus datos de perfil sin cambiar su identificador.

#### Scenario: Modificación válida
- **WHEN** se envían datos válidos para modificar un usuario activo existente
- **THEN** el sistema persiste los cambios y responde con código 200 con la representación actualizada

#### Scenario: Modificar usuario inexistente o inactivo
- **WHEN** se intenta modificar un usuario inexistente o dado de baja
- **THEN** el sistema responde con código 404

### Requirement: Baja lógica de usuarios (Entrega 1)
Cuando se solicita eliminar un usuario activo, el sistema deberá (SHALL) marcarlo inactivo y conservar sus datos, referencias históricas y asociaciones de roles en `usuario_rol`.

#### Scenario: Baja de usuario
- **WHEN** se solicita `DELETE /api/usuarios/{id}` para un usuario activo
- **THEN** el sistema lo marca inactivo, conserva el registro y sus asociaciones de roles en `usuario_rol`, y responde con código 204

#### Scenario: Repetición de baja
- **WHEN** se solicita eliminar un usuario ya inactivo o inexistente
- **THEN** el sistema responde con código 404 y no elimina físicamente ningún registro

### Requirement: Validación de datos de usuario (Entrega 1)
Cuando una solicitud de alta o modificación contiene datos obligatorios inválidos o un DNI ya registrado, el sistema deberá (SHALL) rechazarla sin alterar los datos existentes.

#### Scenario: Campos obligatorios inválidos
- **WHEN** la solicitud omite DNI, nombre, apellido o ciudad, o contiene un DNI no positivo
- **THEN** el sistema responde con código 400 e informa los errores de validación

#### Scenario: DNI duplicado
- **WHEN** se registra o modifica un usuario con un DNI perteneciente a otro usuario, incluso si este fue dado de baja
- **THEN** el sistema responde con código 409 y no crea ni modifica el usuario
