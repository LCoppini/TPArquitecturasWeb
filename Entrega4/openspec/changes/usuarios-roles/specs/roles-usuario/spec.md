# Spec Delta

## Purpose

Establece el catálogo de roles que reconoce TicketYa en Entrega 1 y el contrato para consultar ese catálogo y asociar roles a usuarios.

## ADDED Requirements

### Requirement: Catálogo inicial de roles (Entrega 1)
El sistema deberá (SHALL) disponer de los roles `CLIENTE`, `ORGANIZADOR`, `ADMINISTRADOR` y `PERSONAL_ACCESO` para asociarlos a usuarios, y sus nombres deberán ser únicos.

#### Scenario: Inicialización del catálogo
- **WHEN** se inicializa el servicio o su base de datos
- **THEN** el catálogo permite consultar exactamente los cuatro roles definidos y no contiene duplicados

#### Scenario: Consulta de roles disponibles
- **WHEN** se solicita `GET /api/roles`
- **THEN** el sistema responde con código 200 y los identificadores y nombres de los cuatro roles disponibles

### Requirement: Asignación y remoción de roles de usuario (Entrega 1)
Cuando se asignan roles a un usuario, el sistema deberá (SHALL) aceptar únicamente roles del catálogo y reemplazar de forma idempotente el conjunto de asociaciones de ese usuario por el conjunto solicitado.

#### Scenario: Asignación de varios roles
- **WHEN** se envía `PUT /api/usuarios/{id}/roles` con uno o varios nombres de rol válidos para un usuario activo
- **THEN** el sistema persiste esas asociaciones, elimina las asociaciones anteriores que no estén en la solicitud y devuelve el usuario actualizado con código 200

#### Scenario: Remoción de todos los roles
- **WHEN** se envía una lista vacía a `PUT /api/usuarios/{id}/roles` para un usuario activo
- **THEN** el sistema elimina sus asociaciones de roles y responde con código 200 con una lista de roles vacía

#### Scenario: Asignación repetida
- **WHEN** se repite la misma solicitud de asignación para el mismo usuario
- **THEN** el usuario conserva una sola asociación por cada rol solicitado y el resultado es el mismo

#### Scenario: Rol desconocido
- **WHEN** la solicitud incluye un rol que no pertenece al catálogo
- **THEN** el sistema responde con código 400 y no modifica las asociaciones existentes

#### Scenario: Usuario inexistente o inactivo
- **WHEN** se asignan roles a un usuario inexistente o dado de baja
- **THEN** el sistema responde con código 404 y no modifica ninguna asociación

### Requirement: Catálogo de roles no editable por API (Entrega 1)
La API deberá (SHALL) permitir consultar los roles disponibles, pero no crear, modificar ni eliminar roles.

#### Scenario: Intento de modificar el catálogo
- **WHEN** un cliente solicita crear, modificar o eliminar un rol mediante la API
- **THEN** el sistema no ofrece una operación de escritura para el catálogo y no cambia los roles disponibles
