# Spec Delta

## Purpose

Gestiona el perfil de gustos de cada cliente derivado de sus compras aprobadas: tipos de evento, artistas y ciudad. Garantiza que una compra se registre una sola vez y que el perfil se pueda consultar con las reglas de pertenencia del proyecto.

## ADDED Requirements

### Requirement: Registro idempotente de la compra [Entrega 2]

Cuando se notifica una compra aprobada, el sistema DEBE registrar el tipo de evento y los artistas del evento en la preferencia del cliente, y DEBE hacerlo una sola vez por entrada.

#### Scenario: primera compra del cliente

- **WHEN** llega una notificación de compra aprobada con tipo de evento y artistas
- **THEN** se crea la preferencia del cliente con esos valores en uno
- **AND** la entrada queda registrada como procesada

#### Scenario: reintento con la misma entrada

- **WHEN** llega una segunda notificación con el mismo identificador de entrada
- **THEN** los contadores de tipos y artistas no se incrementan
- **AND** la respuesta es satisfactoria

#### Scenario: artistas repetidos dentro de una compra

- **WHEN** un evento incluye dos veces al mismo artista
- **THEN** el contador de ese artista se incrementa una sola vez

### Requirement: Un artista con una compra ya es favorito [Entrega 2]

El sistema DEBE considerar favorito a todo artista presente en al menos una compra del cliente, sin exigir un mínimo de compras.

#### Scenario: artista con una sola compra

- **WHEN** un artista aparece en una única compra aprobada
- **THEN** el artista queda registrado en la preferencia del cliente
- **AND** puede ser usado como candidato en recomendaciones

### Requirement: Ciudad derivada del usuario [Entrega 2]

Cuando se registra una compra, el sistema DEBE actualizar la ciudad de la preferencia con la ciudad vigente del usuario.

#### Scenario: cambio de ciudad

- **WHEN** el usuario tiene una ciudad distinta de la guardada en su preferencia
- **Y** se aprueba una nueva compra
- **THEN** la preferencia conserva la ciudad actual del usuario

### Requirement: Sin descuento por devolución [Entrega 2]

El sistema NO DEBE implementar notificaciones de devolución hacia preferencias; por lo tanto, una devolución aprobada NO DEBE modificar los contadores de tipos ni artistas.

#### Scenario: devolución aprobada

- **WHEN** se aprueba la devolución de una entrada ya registrada
- **THEN** la preferencia del cliente mantiene sus contadores sin cambios
- **AND** no existe un mecanismo de notificación de devoluciones hacia preferencias

### Requirement: Sin consultas a eventos ni ubicación al registrar [Entrega 2]

Durante el registro de una compra, el sistema NO DEBE consultar el servicio de eventos ni el servicio de ubicación; los datos del evento DEBEN llegar en la notificación.

#### Scenario: registro con datos de la compra

- **WHEN** se procesa la notificación de una compra
- **THEN** el tipo y los artistas se toman del contenido de la notificación
- **AND** no se realizan consultas a eventos ni a ubicación

### Requirement: Consulta de preferencias con pertenencia [Entrega 2]

El sistema DEBE exponer `GET /api/preferencias/clientes/{idUsuario}` para `CLIENTE`, comparando el identificador del path contra `X-User-Id` y respondiendo 403 si difieren. DEBE exponer `GET /api/admin/preferencias/clientes/{idUsuario}` para `ADMINISTRADOR`, sin aplicar esa comparación. El gateway DEBE autorizar cada path por rol, sin propagar el rol al servicio.

#### Scenario: cliente consulta su preferencia

- **WHEN** un cliente llama `GET /api/preferencias/clientes/{idUsuario}` con su propio identificador
- **THEN** el sistema devuelve tipos, artistas y ciudad

#### Scenario: cliente consulta la preferencia de otro

- **WHEN** un cliente llama `GET /api/preferencias/clientes/{idUsuario}` con un identificador distinto al de `X-User-Id`
- **THEN** el sistema responde 403

#### Scenario: administrador consulta cualquier preferencia

- **WHEN** un administrador llama `GET /api/admin/preferencias/clientes/{idUsuario}` con cualquier identificador
- **THEN** el sistema devuelve la preferencia solicitada

#### Scenario: rol usa el path ajeno

- **WHEN** un cliente llama un path `/api/admin/preferencias/**` o un administrador llama `GET /api/preferencias/clientes/{idUsuario}`
- **THEN** el gateway responde 403 antes de reenviar la solicitud
