# Delta de especificación: fila virtual — tope del lote por tanda

## Purpose

El lote de una tanda se mide por las personas asignadas a esa tanda, aunque ya
hayan comprado o su turno haya vencido. Este delta corrige el conteo usado al
habilitar a un recién llegado y deja explícito que `maxPersonas` no se libera
cuando una membresía cambia de estado dentro de la misma tanda.

## ADDED Requirements

### Requirement: Tope de lote por tanda con estados terminales

El lote de una tanda MUST contar todas las membresías asignadas a esa tanda, cualquiera sea su estado. El sistema MUST habilitar a un recién llegado en esa tanda solo si ese total es menor que `maxPersonas`; si no, MUST dejarlo `EN_ESPERA`.

#### Scenario: el lote no supera `maxPersonas` aunque algunos ya hayan comprado
- **GIVEN** una tanda `ABIERTA` con `venceEn` posterior y `cupoRestante` mayor que cero
- **AND** `maxPersonas` es 30 y ya hay 30 personas asignadas a esa tanda, 10 de ellas en estado `COMPRO`
- **WHEN** un nuevo cliente se une
- **THEN** queda `EN_ESPERA` y no se asigna a esa tanda
- **AND** el total de membresías asignadas a la tanda sigue siendo 30

#### Scenario: los turnos vencidos siguen ocupando lugar en el lote
- **GIVEN** una tanda `ABIERTA` con lugar libre en `cupoRestante`
- **AND** 5 de las 30 personas asignadas tienen estado `VENCIDO` y conservan su `idTanda`
- **WHEN** un nuevo cliente se une
- **THEN** queda `EN_ESPERA` porque el lote sigue completo
- **AND** el total de membresías asignadas a la tanda no cambia
