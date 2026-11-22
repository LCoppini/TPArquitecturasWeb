# Spec Delta

## Purpose

Definir la consulta interna del historial de envíos de `msvc-notificaciones` para auditar qué se envió, a quién y con qué resultado.

## ADDED Requirements

### Requirement: Registro de cada notificación [Entrega 2]
El sistema deberá (SHALL) conservar para cada notificación su tipo, usuario, destinatario, asunto, estado, intentos, próximo intento, último error, `correlationId` y fechas de creación y envío.

#### Scenario: Notificación enviada
- **WHEN** una notificación se envía correctamente
- **THEN** el registro conserva el destinatario, el asunto, el estado `ENVIADA` y la fecha de envío

#### Scenario: Notificación fallida
- **WHEN** una notificación falla
- **THEN** el registro conserva el motivo del último error y la cantidad de intentos

### Requirement: Consulta por id [Entrega 2]
El sistema deberá (SHALL) devolver una notificación por su id y deberá (SHALL) responder 404 si no existe.

#### Scenario: Notificación existente
- **WHEN** se consulta `GET /internal/notificaciones/{id}` de una notificación existente
- **THEN** responde 200 con su estado actual

#### Scenario: Notificación inexistente
- **WHEN** se consulta un id que no existe
- **THEN** responde 404

### Requirement: Listado filtrado [Entrega 2]
El sistema deberá (SHALL) listar las notificaciones, opcionalmente filtradas por `idUsuario` y por `estado`, ordenadas de la más reciente a la más antigua y limitadas a 100.

#### Scenario: Filtro por usuario
- **WHEN** se consulta `GET /internal/notificaciones?idUsuario=42`
- **THEN** devuelve solo las notificaciones de ese usuario, la más reciente primero

#### Scenario: Filtro por estado
- **WHEN** se consulta `GET /internal/notificaciones?estado=FALLIDA`
- **THEN** devuelve solo las notificaciones fallidas

#### Scenario: Estado inválido
- **WHEN** el parámetro `estado` no es un estado conocido
- **THEN** responde 400
