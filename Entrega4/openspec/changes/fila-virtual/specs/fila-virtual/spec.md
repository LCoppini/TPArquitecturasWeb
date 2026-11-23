# Delta de especificación: fila virtual

## Purpose

La fila virtual ordena el acceso a la compra de eventos de alta demanda, limita el ingreso por tandas y coordina los turnos y cupos con la disponibilidad real del evento.

## ADDED Requirements

### Requirement: Configuración y activación manual [Entrega 2]

Cuando el organizador propietario active la fila, el sistema MUST validar sus parámetros contra la disponibilidad real y abrir inmediatamente una tanda solo si hay al menos una persona en `EN_ESPERA`.

#### Scenario: activación válida
- **GIVEN** un evento `PUBLICADO` sin entradas reservadas ni vendidas y con disponibilidad real
- **WHEN** su organizador propietario activa la fila con `maxPersonas` y `cupoEntradas` válidos
- **THEN** se crea la fila persistida directamente en ACTIVA
- **AND** se abre inmediatamente una tanda solo si hay personas en `EN_ESPERA`

#### Scenario: activación con fila vacía
- **GIVEN** el evento es elegible y todavía no hay personas en `EN_ESPERA`
- **WHEN** su organizador propietario activa la fila
- **THEN** la fila queda ACTIVA sin tanda en curso
- **AND** no se crea una tanda vacía ni se inicia un ciclo repetido de cierre y apertura

#### Scenario: valores por defecto limitados por inventario
- **GIVEN** un evento `PUBLICADO` sin `maxPersonas` ni `cupoEntradas` informados y con `N` entradas disponibles, donde `N > 0`
- **WHEN** se solicita activar la fila
- **THEN** `maxPersonas = min(20, N)`
- **AND** `cupoEntradas = min(maxPersonas * 2, N)` y nunca es menor que `maxPersonas`
- **AND** si `N = 0`, la activación responde 409 porque no hay inventario disponible

#### Scenario: límites de parámetros informados explícitamente
- **GIVEN** el organizador informa uno o ambos parámetros de activación
- **WHEN** se solicita activar la fila
- **THEN** los valores informados se validan como positivos, `maxPersonas <= cupoEntradas <= maxPersonas * 6` y contra las entradas disponibles
- **AND** cualquier valor explícito que exceda la disponibilidad o los límites responde 400
- **AND** los defaults no se rechazan por superar disponibilidad, porque se limitan a ella

#### Scenario: activación para evento no publicado
- **GIVEN** el evento no está `PUBLICADO`
- **WHEN** su organizador intenta activar la fila
- **THEN** responde 409 y no crea ni activa una fila

#### Scenario: activación con inventario previo
- **GIVEN** un evento con al menos una entrada reservada o vendida
- **WHEN** se intenta activar su fila
- **THEN** la operación responde 409 y no crea ni activa una fila

#### Scenario: disponibilidad remota no disponible durante activación
- **GIVEN** Evento o Ubicación no responde al consultar estado, sectores o capacidades
- **WHEN** se solicita activar la fila
- **THEN** la operación falla cerrada con 503 y no persiste fila ni abre la primera tanda

#### Scenario: parámetros inmutables mientras la fila está activa
- **GIVEN** la fila está ACTIVA
- **WHEN** se intenta modificar `maxPersonas` o `cupoEntradas`
- **THEN** el sistema responde 409 y conserva ambos valores

### Requirement: Unicidad de la fila por evento [Entrega 2]

Cuando se cree o active una fila, el sistema MUST mantener como máximo una fila virtual por evento, incluso ante solicitudes concurrentes o reintentos.

#### Scenario: activaciones concurrentes
- **GIVEN** dos solicitudes intentan crear/activar la fila del mismo evento simultáneamente
- **WHEN** ambas operaciones se procesan
- **THEN** existe una sola fila para el evento
- **AND** una solicitud devuelve el estado vigente o un conflicto, sin duplicar la fila

### Requirement: Transiciones de estado permitidas [Entrega 2]

Cuando se solicite una operación administrativa de estado, el sistema MUST aplicar únicamente las transiciones listadas en la tabla de este documento.

#### Scenario: tabla completa de transiciones
- **GIVEN** la fila está en uno de los estados indicados
- **WHEN** se ejecuta una operación de estado
- **THEN** solo se permiten estas transiciones; `CERRADA` es irreversible y `AGOTADA` requiere reanudación manual

| Estado actual | Operación/condición | Estado resultante | Resultado si no está permitido |
|---|---|---|---|
| ACTIVA | Pausar | PAUSADA | — |
| ACTIVA | Cerrar | CERRADA | — |
| ACTIVA | Abrir tanda sin disponibilidad y sin compras en curso | AGOTADA | — |
| PAUSADA | Reanudar con disponibilidad | ACTIVA | — |
| PAUSADA | Reanudar sin disponibilidad | AGOTADA | — |
| PAUSADA | Cerrar | CERRADA | — |
| PAUSADA | Pausar | Sin cambios | Devuelve el estado actual |
| AGOTADA | Reanudar con disponibilidad | ACTIVA | — |
| AGOTADA | Reanudar sin disponibilidad | AGOTADA | No habilita personas |
| AGOTADA | Cerrar | CERRADA | — |
| AGOTADA | Pausar | Sin cambios | 409 |
| ACTIVA / PAUSADA / AGOTADA / CERRADA | Repetir la operación que deja el mismo estado | Sin cambios | Devuelve el estado actual |
| Cualquier estado persistido | Cancelación del evento | CERRADA | Sin venta libre |
| CERRADA | Activar, pausar o reanudar | Sin cambios | 409 |
| Cualquier otra transición no listada | Operación de estado | Sin cambios | 409 |

`INACTIVA` representa la ausencia de una fila persistida, no una fila almacenada. La activación crea el recurso directamente en `ACTIVA`. Sobre un evento sin fila, pausar, reanudar, cerrar o consultar la administración de la fila responde 404. La tabla de transiciones aplica solo a filas persistidas.

#### Scenario: transición no permitida
- **GIVEN** la fila está en un estado desde el cual la transición solicitada no está permitida
- **WHEN** se solicita esa transición
- **THEN** el sistema responde 409 y conserva el estado actual

#### Scenario: pausar una fila ya pausada
- **GIVEN** la fila ya está PAUSADA
- **WHEN** se solicita pausarla nuevamente
- **THEN** responde con el estado PAUSADA sin cambios

#### Scenario: administrar fila inexistente
- **GIVEN** el evento no tiene una fila persistida (`INACTIVA` lógico)
- **WHEN** el organizador intenta pausar, reanudar o cerrar la fila
- **THEN** el sistema responde 404 porque el recurso fila no existe

#### Scenario: cancelación del evento
- **GIVEN** existe una fila para el evento
- **WHEN** el evento se cancela
- **THEN** la fila pasa a CERRADA con causa de cierre `CANCELACION`
- **AND** ese cierre no habilita venta libre, aunque ya no queden turnos vigentes

#### Scenario: repetición idempotente
- **GIVEN** la fila ya está en el estado que solicita la operación
- **WHEN** se repite activar, pausar, reanudar o cerrar
- **THEN** el sistema no modifica la fila y devuelve su estado actual

#### Scenario: activación repetida o fila cerrada
- **GIVEN** existe una fila persistida en estado ACTIVA o CERRADA
- **WHEN** se solicita activarla nuevamente
- **THEN** si está ACTIVA devuelve el estado actual sin cambios
- **AND** si está CERRADA responde 409

#### Scenario: parámetros de activación sobre fila existente
- **GIVEN** ya existe una fila persistida para el evento
- **WHEN** se intenta crear otra fila mediante la operación de activación
- **THEN** no se duplica la fila y se responde con el estado actual si está ACTIVA, o 409 si está en otro estado

### Requirement: Pausa y reanudación [Entrega 2]

Cuando la fila esté PAUSADA, el sistema MUST detener la apertura de nuevas tandas sin detener los relojes de turnos ya habilitados.

#### Scenario: pausar una tanda en curso
- **GIVEN** una tanda contiene personas con turno vigente
- **WHEN** el organizador pausa la fila
- **THEN** esas personas conservan su vencimiento original
- **AND** las personas en espera conservan su secuencia
- **AND** no se abre una tanda nueva al finalizar la actual mientras la fila permanezca PAUSADA

#### Scenario: reanudar una fila pausada
- **GIVEN** la fila está PAUSADA y no tiene una tanda todavía en curso
- **WHEN** el organizador la reanuda
- **THEN** si hay disponibilidad pasa a ACTIVA y abre una tanda con el remanente acumulado cuando no quede otra tanda en curso
- **AND** si no hay disponibilidad pasa a AGOTADA sin habilitar personas ni alterar secuencias

### Requirement: Cierre irreversible y venta libre grupal [Entrega 2]

Cuando el organizador cierre manualmente la fila, el sistema MUST retirar a quienes esperan, permitir a los turnos vigentes completar dentro de su plazo y habilitar después la compra grupal sin turno solo si el evento sigue `PUBLICADO`.

#### Scenario: cierre con turnos vigentes
- **GIVEN** la fila tiene personas habilitadas con turnos vigentes y personas en espera
- **WHEN** el organizador cierra la fila
- **THEN** las personas en espera salen de la fila sin ser habilitadas
- **AND** las personas habilitadas conservan su vencimiento original y pueden iniciar/completar la compra dentro de ese plazo
- **AND** una persona sin turno vigente no puede iniciar una compra grupal todavía

#### Scenario: venta libre después de vencer todos los turnos
- **GIVEN** la fila está CERRADA y no quedan turnos vigentes
- **AND** el evento sigue `PUBLICADO`
- **WHEN** un cliente solicita una compra grupal
- **THEN** el sistema permite comprar sin turno, sujeto a disponibilidad y al límite de seis entradas
- **AND** cada Compra reservada tiene un vencimiento a diez minutos desde su creación
- **AND** las APIs individuales de reserva y pago siguen bloqueadas para ese evento

#### Scenario: fila cerrada por cancelación
- **GIVEN** el evento se cancela y su fila pasa a CERRADA
- **WHEN** no quedan turnos vigentes y un cliente intenta una Compra sin turno
- **THEN** el sistema no habilita venta libre porque el evento no está `PUBLICADO`
- **AND** las APIs individuales siguen bloqueadas

### Requirement: Ingreso idempotente y orden estable de la fila [Entrega 2]

Cuando un cliente se una a una fila disponible, el sistema MUST asignarle una única secuencia creciente por evento y conservarla mientras siga en esa fila.

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

#### Scenario: ingreso mientras hay una tanda en curso
- **GIVEN** la fila está ACTIVA y ya hay una tanda en curso
- **WHEN** un cliente se une
- **THEN** queda en `EN_ESPERA` para la siguiente tanda
- **AND** no se agrega a la tanda actual aunque esta no haya alcanzado `maxPersonas`

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

### Requirement: Condiciones para unirse [Entrega 2]

Cuando una fila persistida exista, el sistema MUST aceptar nuevos ingresos mientras esté ACTIVA, PAUSADA o AGOTADA, y MUST rechazarlos cuando esté CERRADA; si no existe fila (`INACTIVA` lógico), MUST rechazar el ingreso.

#### Scenario: ingreso durante pausa o agotamiento
- **GIVEN** la fila está PAUSADA o AGOTADA
- **WHEN** un cliente solicita unirse
- **THEN** el sistema lo agrega al final con una secuencia nueva, sin habilitarlo

#### Scenario: ingreso sin fila o en estado cerrado
- **GIVEN** el evento no tiene una fila persistida o la fila está CERRADA
- **WHEN** un cliente solicita unirse
- **THEN** la solicitud responde 404 si no existe fila persistida o 409 si la fila está CERRADA
- **AND** no se crea una entrada de fila

#### Scenario: reingreso después de completar la compra
- **GIVEN** una persona completó una compra y su membresía quedó en estado terminal `COMPRO`
- **AND** la fila sigue ACTIVA, PAUSADA o AGOTADA
- **WHEN** solicita unirse nuevamente
- **THEN** se crea una nueva membresía al final con una secuencia nueva
- **AND** la membresía histórica `COMPRO` permanece inactiva y no vuelve a habilitarse

### Requirement: Cierre de tanda [Entrega 2]

El sistema MUST cerrar una tanda cuando venza su turno de diez minutos, cuando todas las personas habilitadas compren o salgan sin Compras `RESERVADA`, o cuando el cupo llegue a cero sin Compras `RESERVADA`. No debe cerrar anticipadamente por resolución completa o cupo cero mientras quede una Compra `RESERVADA`. Solo MUST abrir una tanda si existe al menos una persona en `EN_ESPERA`.

#### Scenario: cierre anticipado porque todos resolvieron
- **GIVEN** todas las personas habilitadas tienen una Compra pagada o salieron de la fila y no quedan Compras `RESERVADA` de la tanda
- **WHEN** se verifica el estado de la tanda
- **THEN** la tanda se cierra sin esperar los diez minutos completos
- **AND** se abre inmediatamente la siguiente tanda si la fila está ACTIVA, hay personas en `EN_ESPERA` y hay disponibilidad
- **AND** si no hay personas esperando, la fila permanece ACTIVA sin tanda en curso

#### Scenario: el cupo se agota sin Compras pendientes
- **GIVEN** el cupo restante llegó a cero y no queda ninguna Compra `RESERVADA` de la tanda
- **WHEN** se verifica el estado de la tanda
- **THEN** la tanda se cierra porque nadie puede recibir cupo devuelto
- **AND** quienes recibieron `CUPO_TANDA_INSUFICIENTE` vuelven a `EN_ESPERA` conservando su secuencia original
- **AND** todos los habilitados que no compraron vuelven a `EN_ESPERA` conservando su secuencia original, aunque no hayan recibido ese rechazo
- **AND** se abre la siguiente tanda inmediatamente si la fila está ACTIVA y hay disponibilidad

#### Scenario: la Compra pendiente impide el cierre anticipado
- **GIVEN** se cumpliría el cierre por resolución completa o cupo agotado, pero existe una Compra `RESERVADA` de la tanda
- **WHEN** se verifica el estado de la tanda
- **THEN** la tanda no se cierra por esas condiciones mientras la Compra pueda vencer y devolver cupo

#### Scenario: cierre por vencimiento de diez minutos
- **GIVEN** transcurrieron diez minutos desde el inicio de la tanda
- **WHEN** el proceso de cierre obtiene el lock de fila del evento
- **THEN** vence y compensa en la misma transacción todas las Compras `RESERVADA` de esa tanda que sigan pendientes
- **AND** solo después cierra la tanda
- **AND** el cierre no depende de que el `@Scheduled` haya corrido previamente
- **WHEN** se completa el cierre
- **THEN** se cierra la tanda
- **AND** se abre inmediatamente la siguiente si la fila está ACTIVA y hay disponibilidad

#### Scenario: reevaluar cierre al resolver una Compra
- **GIVEN** una Compra de la tanda cambia a `PAGADA`, `VENCIDA` o `RECHAZADA`
- **WHEN** se confirma esa transición
- **THEN** el servicio reevalúa bajo el lock de fila si se cumple una condición de cierre y apertura
- **AND** un `@Scheduled` respalda la evaluación por vencimiento de tiempo

#### Scenario: una Compra rechazada no resuelve el turno
- **GIVEN** una Compra fue rechazada definitivamente y compensada, pero su persona conserva el turno vigente
- **WHEN** se verifica el cierre por resolución completa
- **THEN** esa persona no se considera resuelta por el rechazo
- **AND** puede iniciar otra Compra dentro del mismo turno

#### Scenario: turno con rechazo de cupo conserva secuencia al vencer por tiempo
- **GIVEN** una persona habilitada recibió `CUPO_TANDA_INSUFICIENTE` y no logró comprar
- **WHEN** vence el turno por alcanzar los diez minutos
- **THEN** vuelve a `EN_ESPERA` conservando su secuencia original
- **AND** no pierde su lugar por el vencimiento porque el rechazo de cupo preserva esa prioridad

#### Scenario: disponibilidad real
- **GIVEN** hay entradas reservadas o vendidas, o se liberaron entradas por vencimiento/devolución
- **WHEN** se calcula el cupo efectivo de una tanda
- **THEN** las reservadas y vendidas se excluyen de `entradasDisponibles`
- **AND** las entradas liberadas se incluyen en el siguiente recálculo
- **AND** el cupo nunca habilita reservas por encima de la disponibilidad real

#### Scenario: no hay disponibilidad al abrir
- **GIVEN** corresponde abrir una tanda y no quedan entradas disponibles ni compras en curso
- **WHEN** el sistema evalúa la apertura
- **THEN** no habilita a ninguna persona y la fila pasa a AGOTADA
- **AND** las personas en espera conservan su secuencia

#### Scenario: cierre y apertura atómicos e idempotentes
- **GIVEN** dos instancias procesan a la vez el mismo cierre de tanda por evento duplicado o tick programado
- **WHEN** ambas verifican cierre y apertura bajo el lock de fila del evento y actualizan condicionalmente el estado
- **THEN** como máximo una tanda nueva queda abierta
- **AND** un evento o tick duplicado no repite el cierre ni la apertura

#### Scenario: pausa detiene aperturas, no turnos
- **GIVEN** la fila está PAUSADA cuando una tanda se cierra
- **WHEN** correspondería abrir la siguiente
- **THEN** no se abre una tanda durante la pausa aunque continúen corriendo los relojes
- **AND** al reanudar se abre inmediatamente si no hay tanda en curso y hay disponibilidad

### Requirement: Apertura de tanda y cálculo de cupo [Entrega 2]

Al cerrar una tanda, el sistema MUST abrir inmediatamente la siguiente si la fila está ACTIVA y hay disponibilidad, habilitando hasta `maxPersonas` y asignando `cupoEfectivo = min(cupoEntradas + remanenteAnterior, entradasDisponibles)`.

#### Scenario: primera tanda y tandas sucesivas
- **GIVEN** la fila se activa o la tanda anterior se cerró con la fila ACTIVA, sin otra tanda en curso y con personas en `EN_ESPERA`
- **WHEN** se abre una tanda
- **THEN** se habilita en orden de secuencia a no más de `maxPersonas` personas
- **AND** el cupo efectivo se calcula con la fórmula definida
- **AND** `cupoEntradas` no supera `maxPersonas * 6`

#### Scenario: no se abre una tanda sin personas esperando
- **GIVEN** la fila está ACTIVA sin una tanda en curso y no hay personas en `EN_ESPERA`
- **WHEN** se evalúa abrir una tanda
- **THEN** no se crea una tanda vacía y la fila permanece ACTIVA
- **AND** no se repite el cierre por resolución completa

#### Scenario: falla la consulta remota al abrir una tanda
- **GIVEN** Evento o Ubicación no responde al obtener datos requeridos para calcular disponibilidad
- **WHEN** corresponde abrir una tanda
- **THEN** la apertura falla cerrada, no crea tanda ni habilita personas y conserva el estado actual de la fila
- **AND** el scheduler reintenta la apertura posteriormente
- **AND** ninguna llamada Feign ocurre mientras está tomado el lock de fila del evento

#### Scenario: cierre de tanda concurrente con creación de Compra
- **GIVEN** el cierre de una tanda y la creación de una Compra para esa tanda ocurren concurrentemente
- **WHEN** ambas operaciones siguen el orden global de locks y la actualización condicional de la tanda
- **THEN** la Compra queda asociada a la tanda abierta antes del cierre, o se rechaza si el cierre ganó
- **AND** no queda una Compra nueva asociada a una tanda cerrada ni cupo/asientos parcialmente reservados

### Requirement: Reutilización del cupo devuelto [Entrega 2]

Cuando una compra vencida libere cupo de una tanda todavía en curso, el sistema MUST devolverlo al cupo de esa tanda sin habilitar a personas nuevas.

#### Scenario: cupo liberado durante la tanda
- **GIVEN** una compra vence y hay personas ya habilitadas en la tanda
- **WHEN** se compensa la compra
- **THEN** el cupo vuelve al total de la tanda y puede asignarse atómicamente a solicitudes de compra recibidas de personas ya habilitadas
- **AND** no se habilita a nadie nuevo en ese momento
- **AND** una persona cuyo turno venció no recupera su lugar

#### Scenario: remanente al cerrar una tanda
- **GIVEN** una tanda finaliza con cupo de entradas sin usar
- **WHEN** se abre la próxima tanda
- **THEN** el remanente se suma a `cupoEntradas` antes de limitarlo por disponibilidad real

### Requirement: Preservación del turno ante cupo insuficiente [Entrega 2]

Cuando un habilitado intente comprar y la tanda no tenga cupo suficiente, el sistema MUST registrar el rechazo `CUPO_TANDA_INSUFICIENTE`. Si la tanda cierra por cupo agotado, todos los habilitados que no compraron vuelven a `EN_ESPERA` con su secuencia original, hayan recibido o no ese rechazo. Solo al cierre por vencimiento de diez minutos pierde su lugar quien no compró ni registró el rechazo.

#### Scenario: compra rechazada por falta de cupo
- **GIVEN** una persona tiene turno vigente y su intento de compra excede el cupo restante
- **WHEN** la API rechaza la operación con `CUPO_TANDA_INSUFICIENTE`
- **THEN** persiste el intento vinculado a la persona y la tanda
- **AND** no crea Compra ni reserva entradas, asientos o cupo
- **AND** la persona conserva el turno hasta que cierre la tanda

#### Scenario: cupo devuelto durante turno conservado
- **GIVEN** una persona recibió `CUPO_TANDA_INSUFICIENTE` y su turno sigue vigente
- **AND** otra Compra libera cupo antes del cierre de la tanda
- **WHEN** la persona vuelve a solicitar una compra válida
- **THEN** puede competir por el cupo devuelto mediante la asignación atómica de la tanda
- **AND** no se habilita a otra persona en su lugar

#### Scenario: conservar secuencia al cierre de tanda
- **GIVEN** una persona habilitada no logró comprar antes de que la tanda cierre por cupo agotado, haya recibido o no `CUPO_TANDA_INSUFICIENTE`
- **WHEN** la tanda cierra
- **THEN** su estado vuelve a `EN_ESPERA` conservando la secuencia original
- **AND** queda delante de personas que se unieron posteriormente

#### Scenario: perder el turno por vencimiento sin comprar
- **GIVEN** una persona habilitada no compró y no recibió `CUPO_TANDA_INSUFICIENTE`
- **WHEN** el turno vence por tiempo
- **THEN** sale de la fila y no conserva su secuencia
- **AND** debe unirse otra vez para recibir una secuencia nueva

### Requirement: Orden transaccional de asignación de cupo [Entrega 2]

Cuando varias solicitudes concurrentes compitan por cupo, el sistema MUST asignarlo atómicamente según el orden de adquisición del lock de la tanda en PostgreSQL.

#### Scenario: solicitudes concurrentes
- **GIVEN** varias solicitudes habilitadas compiten por el cupo restante
- **WHEN** adquieren el lock de la tanda en un orden determinado
- **THEN** se asigna cupo atómicamente en ese orden y ninguna solicitud excede el saldo
- **AND** el orden no garantiza FIFO por instante de llegada a la red

#### Scenario: FIFO durable fuera de alcance
- **GIVEN** se requiere garantizar estrictamente orden FIFO por llegada de red incluso con concurrencia y varias instancias
- **WHEN** se considera incorporar una cola durable de solicitudes
- **THEN** esa cola queda fuera de alcance de este change

### Requirement: Autorización y propiedad para administrar la fila [Entrega 2]

Cuando se administre la fila, el gateway MUST exigir el rol ORGANIZADOR y `msvc-entrada` MUST verificar que la identidad autenticada sea propietaria del evento.

#### Scenario: organizador propietario
- **GIVEN** el JWT autenticado tiene rol ORGANIZADOR y su `X-User-Id` coincide con `idOrganizador` del evento
- **WHEN** activa, pausa, reanuda o cierra la fila
- **THEN** la operación puede continuar

#### Scenario: organizador no propietario
- **GIVEN** el JWT tiene rol ORGANIZADOR pero su identidad no coincide con `idOrganizador`
- **WHEN** intenta administrar la fila
- **THEN** `msvc-entrada` responde 403 y no modifica la fila

#### Scenario: evento inexistente
- **GIVEN** `msvc-entrada` consulta la propiedad a `msvc-evento`
- **WHEN** Evento informa que el evento no existe
- **THEN** responde 404 sin modificar la fila

#### Scenario: servicio de eventos no disponible
- **GIVEN** `msvc-entrada` consulta la propiedad a `msvc-evento`
- **WHEN** `msvc-evento` no está disponible
- **THEN** responde 503 y falla cerrado, sin modificar la fila

#### Scenario: rol administrador fuera de alcance
- **GIVEN** una persona tiene únicamente rol ADMINISTRADOR
- **WHEN** intenta administrar la fila
- **THEN** el gateway no la autoriza en este change

#### Scenario: denegación de roles en rutas de fila y Compra
- **GIVEN** las rutas de ingreso/Compra requieren `CLIENTE`, administración de fila requiere `ORGANIZADOR` y resolución/reintento administrativo de devoluciones requiere `ADMINISTRADOR`
- **WHEN** una persona con rol `ORGANIZADOR` intenta unirse a la fila o comprar, una persona `CLIENTE` intenta administrar la fila, o una persona que no es `ADMINISTRADOR` intenta resolver/reintentar una devolución
- **THEN** el gateway deniega cada solicitud antes de que `msvc-entrada` ejecute la operación
- **AND** solo `ADMINISTRADOR` puede resolver o reintentar devoluciones de Compra

### Requirement: Coordinación de activación e ingreso a reservas individuales [Entrega 2]

Cuando se active una fila o se creen reservas individuales para el evento, el sistema MUST serializar ambas operaciones de modo que solo una política de venta pueda prevalecer.

#### Scenario: reserva individual obtiene coordinación primero
- **GIVEN** una reserva individual y la activación compiten para el mismo evento
- **WHEN** la reserva gana la coordinación
- **THEN** la activación observa la reserva y responde 409 sin activar la fila

#### Scenario: activación obtiene coordinación primero
- **GIVEN** una reserva individual y la activación compiten para el mismo evento
- **WHEN** la activación gana la coordinación
- **THEN** la fila se activa y la reserva individual posterior se rechaza

### Requirement: Ruteo de las APIs de fila y compra [Entrega 2]

Cuando el gateway reciba solicitudes bajo las rutas públicas de fila virtual, su administración o Compra, el sistema MUST enrutarlas a `msvc-entrada`.

#### Scenario: rutas públicas de fila y compra
- **GIVEN** `msvc-entrada` está registrado y disponible en Eureka
- **WHEN** el cliente llama `/api/filas-virtuales/**`, `/api/compras/**` o `/api/organizador/filas-virtuales/**`
- **THEN** el gateway enruta la solicitud a `msvc-entrada` y conserva método, path, query, body e identidad autenticada
- **AND** la ruta de administración no colisiona con `/api/organizador/eventos/**` de `msvc-evento`

#### Scenario: destino de fila o compra no disponible
- **GIVEN** una ruta de fila o compra está configurada pero Eureka no ofrece una instancia saludable de `msvc-entrada`
- **WHEN** el cliente realiza la solicitud
- **THEN** el gateway responde 503 con su formato uniforme de error
