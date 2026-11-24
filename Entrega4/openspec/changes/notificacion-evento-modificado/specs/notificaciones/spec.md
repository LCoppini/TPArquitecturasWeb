## ADDED Requirements

### Requirement: Evento modificado
#### Scenario: Cambio de fecha, lugar o nombre
- **WHEN** se actualiza un evento y cambia alguno de esos datos
- **THEN** cada usuario con entradas VALIDA recibe EVENTO_MODIFICADO con el nombre del evento y la descripción de los cambios

#### Scenario: Sin cambios
- **WHEN** la actualización no cambia nombre, fecha ni lugar
- **THEN** no se avisa a nadie

#### Scenario: Evento sin compradores
- **THEN** no se envía ningún correo

#### Scenario: Falla la coordinación
- **THEN** la edición se completa igual y el error queda en el log
