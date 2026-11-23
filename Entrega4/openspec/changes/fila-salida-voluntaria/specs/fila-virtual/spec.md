## ADDED Requirements

### Requirement: Salir de la fila
El cliente EN_ESPERA puede abandonar la fila.

#### Scenario: Salida en espera
- **WHEN** un cliente EN_ESPERA invoca DELETE /ingresos/mi
- **THEN** su ingreso pasa a SALIO y deja de contar en las posiciones de los demás

#### Scenario: Salida repetida
- **WHEN** invoca de nuevo
- **THEN** recibe el mismo estado SALIO sin error

#### Scenario: Cliente habilitado
- **WHEN** el cliente está HABILITADO
- **THEN** responde 409

#### Scenario: Sin ingreso
- **THEN** responde 404

### Requirement: Espera estimada
`mi-estado` devuelve `esperaEstimadaSegundos` para el cliente EN_ESPERA cuando hay una tanda abierta.

#### Scenario: Posición con tandas previas
- **GIVEN** maxPersonas 2, posición 5 y una tanda abierta que vence en 5 minutos
- **THEN** la espera estimada es la suma de lo que resta de esa tanda y 2 tandas completas
