# Fila virtual Specification

## Purpose

La fila virtual persiste el orden de ingreso por evento y habilita turnos por
tandas para que la compra se realice con un turno vigente. Esta spec describe la
habilitación directa al ingresar cuando ya hay una tanda útil abierta, el tope
de `maxPersonas`, el orden FIFO y la revalidación atómica del cupo al comprar.

## Requirements

### Requirement: Ingreso idempotente y orden estable de la fila [Entrega 2]

Cuando un cliente se una a una fila disponible, el sistema MUST asignarle una única secuencia creciente por evento y conservarla mientras siga en esa fila. El orden de habilitación MUST respetar esa secuencia, incluso con varias instancias y bajo demanda concurrente.

#### Scenario: primer ingreso e ingreso repetido
- **GIVEN** la fila no está INACTIVA ni CERRADA
- **WHEN** un cliente solicita unirse por primera vez o reintenta su solicitud
- **THEN** el sistema devuelve su posición y estado
- **AND** un reintento no crea una segunda posición ni cambia su secuencia

#### Scenario: ingreso sin tanda en curso
- **GIVEN** la fila está ACTIVA y no hay una tanda en curso
- **WHEN** un cliente se une
- **THEN** se registra su membresía y se abre una tanda inmediatamente
- **AND** se habilita a las personas en orden de secuencia hasta `maxPersonas`, incluida la nueva membresía si corresponde

#### Scenario: tope de `maxPersonas` bajo demanda concurrente
- **GIVEN** `maxPersonas` es 8 y 12 clientes se unen de forma concurrente mientras hay una tanda abierta con cupo suficiente
- **WHEN** se procesan los 12 ingresos
- **THEN** exactamente 8 quedan `HABILITADOS` en esa tanda y 4 `EN_ESPERA`
- **AND** los 8 habilitados son quienes obtuvieron las secuencias menores

#### Scenario: orden de llegada respetado
- **GIVEN** una persona ya está `EN_ESPERA` con secuencia menor y la tanda abierta tiene lugar libre
- **WHEN** se une un cliente nuevo
- **THEN** el recién llegado queda `EN_ESPERA` y no adelanta a quien ya esperaba
- **AND** el turno de quien esperaba no se le asigna al recién llegado

### Requirement: Habilitación directa con turno útil

Si al ingresar existe una tanda `ABIERTA` con `venceEn` posterior, `cupoRestante` mayor que cero, menos de `maxPersonas` habilitados y ninguna persona `EN_ESPERA` con secuencia menor, el sistema MUST habilitar al recién llegado en esa tanda. En cualquier otro caso MUST dejarlo `EN_ESPERA`, sin exigir un mínimo de tiempo restante.

#### Scenario: ingreso mientras hay una tanda en curso con turno útil
- **GIVEN** la fila está ACTIVA y ya hay una tanda `ABIERTA`
- **AND** su `venceEn` es posterior al momento del ingreso y su `cupoRestante` es mayor que cero
- **AND** la cantidad de personas habilitadas en esa tanda es menor que `maxPersonas`
- **AND** no hay ninguna persona `EN_ESPERA` con secuencia menor que la del recién llegado
- **WHEN** el cliente se une
- **THEN** se registra en estado `HABILITADO`, con el `idTanda` y el `venceEn` de esa tanda
- **AND** puede usar el turno de inmediato sin esperar a que se cierre y abra otra tanda
- **AND** el cupo real de entradas sigue siendo decidido por la reserva atómica al comprar

#### Scenario: turno útil sin umbral de tiempo adicional
- **GIVEN** una tanda `ABIERTA` con `venceEn` posterior al ingreso y `cupoRestante` mayor que cero
- **AND** quedan lugares en el lote de `maxPersonas` y nadie con secuencia menor está `EN_ESPERA`
- **WHEN** el cliente se une
- **THEN** se habilita en esa tanda sin requerir nueva configuración ni un mínimo de tiempo restante
- **AND** su compra posterior sigue sujeta a la reserva atómica de cupo

#### Scenario: ingreso mientras hay una tanda en curso sin turno útil
- **GIVEN** hay una tanda `ABIERTA` pero vencida, o sin cupo restante, o con `maxPersonas` habilitados
- **OR** hay una persona `EN_ESPERA` con secuencia menor que la del recién llegado
- **WHEN** el cliente se une
- **THEN** queda en `EN_ESPERA` para una tanda posterior
- **AND** conserva la secuencia que se le asignó al ingresar

### Requirement: Reserva atómica del cupo al comprar

La habilitación MUST NOT reservar ni modificar el cupo de entradas. Cada compra MUST revalidar el cupo de forma atómica sobre la tanda vigente; si no alcanza, MUST rechazarla por cupo insuficiente sin vender entradas por encima del cupo.

#### Scenario: N concurrentes sobre capacidad N
- **GIVEN** `maxPersonas` es N y `cupoEntradas` es 2N
- **AND** N clientes se unen y compran 2 entradas cada uno de forma concurrente
- **WHEN** todos intentan comprar sin esperar una tanda posterior
- **THEN** las N compras se reservan y pagan con éxito
- **AND** se emiten 2N entradas, con 0 rechazos por falta de turno y 0 sobreventa

#### Scenario: demanda mayor al cupo de entradas
- **GIVEN** `cupoEntradas` es C y más clientes habilitados intentan comprar entradas de las que caben en C
- **WHEN** se procesan todas las compras
- **THEN** se venden exactamente C entradas
- **AND** los rechazos son por cupo insuficiente, no por ausencia de turno
- **AND** no se vende ninguna entrada por encima de C

#### Scenario: secuencia conservada tras falta de cupo
- **GIVEN** un cliente habilitado conserva su turno porque no alcanzó el cupo de entradas
- **WHEN** vuelve a EN_ESPERA para la próxima tanda
- **THEN** conserva su secuencia original sin reasignación
- **AND** queda delante de cualquier persona que se haya unido posteriormente

#### Scenario: el turno vencido requiere un nuevo ingreso
- **GIVEN** el turno de un cliente venció sin que reservara
- **WHEN** consulta o intenta usar ese turno vencido
- **THEN** la API informa claramente que el turno venció y que puede volver a unirse
- **AND** no lo vuelve a agregar automáticamente
- **AND** si solicita unirse nuevamente cuando está permitido, recibe una secuencia nueva al final de la fila

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
