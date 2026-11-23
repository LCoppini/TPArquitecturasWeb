# Delta de especificación: compra agrupada

## Purpose

La compra agrupada permite reservar de forma indivisible hasta seis entradas de un mismo evento y completar su pago en una sola operación, sin perder la identidad ni el precio individual de cada entrada.

## ADDED Requirements

### Requirement: Compatibilidad del flujo individual para eventos sin fila [Entrega 1]

Cuando un evento no tenga una fila virtual activada, el sistema MUST conservar la reserva individual por `POST /api/entradas` y el pago individual por `POST /api/pagos`.

#### Scenario: reserva y pago individuales sin fila
- **GIVEN** un evento sin fila virtual activada
- **WHEN** un cliente crea una reserva individual y luego paga por los endpoints existentes
- **THEN** el flujo conserva el comportamiento actual de una entrada por operación

### Requirement: Compra grupal para eventos con fila [Entrega 2]

Cuando exista una fila persistida para un evento, el sistema MUST exigir una compra grupal para reservar entradas, incluso cuando la fila esté CERRADA y permita venta libre.

#### Scenario: endpoint individual vedado en cualquier estado persistido
- **GIVEN** existe una fila persistida para el evento en estado ACTIVA, PAUSADA, AGOTADA o CERRADA
- **WHEN** un cliente intenta reservar por `POST /api/entradas` o pagar por `POST /api/pagos`
- **THEN** el sistema rechaza la operación con 409
- **AND** no crea reserva ni pago individual

#### Scenario: fila inexistente conserva API individual
- **GIVEN** no existe una fila persistida para el evento (`INACTIVA` lógico)
- **WHEN** un cliente usa los endpoints individuales existentes
- **THEN** el sistema conserva el flujo de Entrega 1

#### Scenario: compra sin turno en venta libre cerrada
- **GIVEN** la fila se cerró manualmente, el evento sigue `PUBLICADO` y no quedan turnos vigentes
- **WHEN** un cliente inicia una compra agrupada
- **THEN** puede comprar sin turno por el flujo de Compra, sujeto a disponibilidad real

#### Scenario: fila cerrada por cancelación
- **GIVEN** la fila está CERRADA por cancelación o el evento no está `PUBLICADO`
- **AND** no quedan turnos vigentes
- **WHEN** un cliente inicia una compra sin turno
- **THEN** la API responde 409 porque el evento no está `PUBLICADO`
- **AND** no reserva entradas

#### Scenario: solo turno vigente inicia compra durante la fila
- **GIVEN** la fila no está en venta libre posterior a un cierre manual
- **WHEN** una persona sin turno vigente intenta crear una Compra
- **THEN** la API responde 409 sin reservar entradas ni cupo
- **AND** una persona con turno vigente puede tener como máximo una Compra `RESERVADA` a la vez en ese turno

#### Scenario: compra libre tiene reserva de diez minutos
- **GIVEN** la fila se cerró manualmente, el evento sigue `PUBLICADO` y no quedan turnos vigentes
- **WHEN** se crea una Compra sin turno
- **THEN** la Compra vence diez minutos después de su creación
- **AND** ese plazo no modifica los vencimientos de turnos anteriores

#### Scenario: solo una Compra libre pendiente por cliente y evento
- **GIVEN** la fila se cerró manualmente, el evento sigue `PUBLICADO`, no quedan turnos vigentes y el cliente ya tiene una Compra `RESERVADA` para ese evento
- **WHEN** intenta crear otra Compra libre con una clave nueva
- **THEN** la API responde 409 y no crea otra Compra ni reserva entradas
- **AND** una vez que la anterior queda pagada, vencida o rechazada, puede crear una nueva Compra de hasta seis entradas con su propio vencimiento de diez minutos

#### Scenario: Idempotency-Key se verifica antes que la Compra pendiente
- **GIVEN** la clave de idempotencia ya identifica una Compra con el mismo payload y esa Compra puede estar vencida
- **WHEN** se reintenta `POST /api/compras`
- **THEN** se devuelve la Compra existente sin crear otra ni responder conflicto por el índice de Compra pendiente

#### Scenario: Compra reservada vencida se compensa al crear otra
- **GIVEN** el cliente tiene una Compra `RESERVADA` del mismo evento con `vence_en <= now()`
- **WHEN** envía una nueva solicitud de Compra con una clave nueva
- **THEN** la API vence la Compra anterior y compensa sus asientos y cupo dentro de la misma transacción
- **AND** después intenta crear la nueva Compra
- **AND** si la Compra anterior todavía está vigente, la nueva solicitud responde 409

### Requirement: Una compra agrupa hasta seis entradas del mismo evento [Entrega 2]

Cuando un cliente con permiso vigente inicie una compra, el sistema MUST aceptar entre una y seis entradas del mismo evento en una única Compra, incluso si pertenecen a distintos sectores o asientos.

#### Scenario: compra de varios sectores
- **GIVEN** un cliente con turno vigente y asientos disponibles en más de un sector
- **WHEN** solicita una compra con hasta seis entradas del mismo evento
- **THEN** se crea una sola Compra con las entradas solicitadas y el precio oficial individual de cada una

#### Scenario: cantidad fuera de límites
- **GIVEN** un cliente envía una compra sin entradas o con más de seis
- **WHEN** solicita crear la Compra
- **THEN** la solicitud responde 400 y no crea Compra ni entradas

#### Scenario: Compra rechazada permite reintentar dentro del turno
- **GIVEN** la Compra anterior del cliente fue rechazada definitivamente y sus reservas y cupo ya fueron compensados
- **AND** el turno del cliente sigue vigente
- **WHEN** inicia otra Compra con una nueva clave de idempotencia
- **THEN** se permite crear otra Compra de hasta seis entradas
- **AND** el nuevo intento no extiende el vencimiento original del turno

#### Scenario: solo una Compra pendiente por cliente y evento
- **GIVEN** el cliente ya tiene una Compra `RESERVADA` vigente para ese evento
- **WHEN** intenta crear otra Compra con otra clave de idempotencia
- **THEN** la API responde 409 y no crea otra Compra ni reserva entradas

#### Scenario: solo el propietario puede consultar, pagar o devolver una Compra
- **GIVEN** el JWT autenticado provee un `X-User-Id` distinto de `Compra.idUsuario`
- **WHEN** intenta consultar, pagar o devolver esa Compra
- **THEN** cada operación responde 404 para no revelar la existencia de la Compra
- **AND** no modifica la Compra, sus entradas ni su pago

### Requirement: Reserva atómica de las entradas de una Compra [Entrega 2]

Cuando una Compra incluya varias entradas, el sistema MUST reservar el conjunto como una unidad y no persistir ninguna entrada si una validación, asiento o capacidad falla.

#### Scenario: una entrada del grupo no está disponible
- **GIVEN** una solicitud de Compra contiene varios asientos y uno ya está ocupado o no pertenece al evento
- **WHEN** se valida y reserva la Compra
- **THEN** la operación se rechaza y no queda ninguna entrada reservada por esa Compra

#### Scenario: compra reservada en su totalidad
- **GIVEN** todos los asientos, sectores y límites por cliente son válidos
- **WHEN** se crea la Compra
- **THEN** todas las entradas quedan reservadas bajo la misma Compra
- **AND** cada entrada conserva su asiento y su precio individual

### Requirement: Cupo de entradas insuficiente en la tanda [Entrega 2]

Cuando la cantidad solicitada supere el cupo todavía disponible en la tanda, el sistema MUST rechazar sin reservar y comunicar cuántas entradas quedan para que el cliente pueda confirmar una cantidad menor.

#### Scenario: cantidad solicitada excede el cupo
- **GIVEN** el turno está vigente y el cupo restante es menor que la cantidad solicitada
- **WHEN** el cliente inicia la Compra
- **THEN** responde 409 con la cantidad de entradas que puede solicitar como máximo en ese instante
- **AND** no crea Compra, entrada ni reserva de cupo
- **AND** registra el intento rechazado con `CUPO_TANDA_INSUFICIENTE` para conservar el turno hasta el cierre de la tanda

#### Scenario: cliente confirma una cantidad menor
- **GIVEN** el cliente recibió el cupo restante informado por la API
- **WHEN** reenvía una solicitud válida con esa cantidad o menos y el cupo continúa disponible
- **THEN** el sistema reserva el conjunto atómicamente

### Requirement: Un solo pago por Compra [Entrega 2]

Cuando se pague una Compra, el sistema MUST procesar un único pago por el total de sus entradas y confirmar todas las entradas juntas o ninguna.

#### Scenario: pago aprobado
- **GIVEN** una Compra vigente con varias entradas reservadas
- **WHEN** se solicita pagarla con una clave de idempotencia válida
- **THEN** se registra un solo pago por la suma de los precios individuales
- **AND** todas las entradas pasan a estado `VALIDA` y reciben su QR individual
- **AND** el pago queda asociado a todas las entradas en el historial de pago

#### Scenario: pago rechazado o falla definitiva
- **GIVEN** una Compra con varias entradas reservadas
- **WHEN** el proveedor rechaza definitivamente el pago o informa una falla terminal
- **THEN** no se emite ninguna entrada
- **AND** se compensan juntas las reservas de asientos y el cupo de la tanda
- **AND** la Compra termina en estado terminal de pago rechazado
- **AND** el turno permanece vigente hasta su vencimiento y el cliente puede iniciar otra Compra dentro de ese mismo turno
- **AND** el vencimiento original no se extiende

#### Scenario: pago de una Compra vencida
- **GIVEN** la Compra tiene `vence_en <= now()` y continúa registrada en estado `RESERVADA`
- **WHEN** el cliente intenta pagarla
- **THEN** la operación vence y compensa la Compra si aún no lo hizo
- **AND** responde con error y no inicia ni registra un cargo
- **AND** no emite entradas ni QR

#### Scenario: pago en curso reintentado
- **GIVEN** el intento de pago de una Compra continúa en curso y su plazo no venció
- **WHEN** el cliente reintenta con la misma clave de idempotencia y los mismos datos
- **THEN** el sistema recupera el resultado del intento existente sin iniciar un cargo duplicado
- **AND** el vencimiento de la Compra no se extiende

### Requirement: Una Compra hereda el reloj del turno [Entrega 2]

Cuando se cree una Compra durante un turno, el vencimiento de la Compra MUST ser exactamente el del turno y el pago debe concluir antes de ese vencimiento.

#### Scenario: compra no extiende el turno
- **GIVEN** una persona inicia la Compra dentro de su turno vigente
- **WHEN** la Compra se crea
- **THEN** su vencimiento coincide con el vencimiento original de la habilitación
- **AND** crear la Compra no inicia ni extiende un plazo propio

#### Scenario: vencimiento antes del pago
- **GIVEN** una Compra sigue sin pago aprobado al alcanzar el vencimiento heredado
- **WHEN** se procesa el vencimiento
- **THEN** se liberan todas sus reservas y el cupo correspondiente
- **AND** no se emiten entradas
- **AND** la persona debe unirse otra vez para recuperar acceso a una fila abierta

### Requirement: Resolución atómica entre vencimiento y pago [Entrega 2]

Cuando el pago de una Compra compita con su vencimiento, el sistema MUST permitir que solo una transición condicional gane.

#### Scenario: gana el vencimiento
- **GIVEN** la Compra está pendiente y vence mientras se procesa la confirmación de pago
- **WHEN** la actualización condicional de vencimiento gana
- **THEN** la Compra queda vencida, todas sus reservas y su cupo se compensan y no se emiten entradas
- **AND** un pago que no pueda confirmarse dentro de ese plazo no convierte la Compra en pagada

#### Scenario: gana la confirmación del pago
- **GIVEN** la Compra está pendiente y vigente cuando se confirma el pago
- **WHEN** la actualización condicional de pago gana
- **THEN** la Compra queda pagada y todas sus entradas se confirman juntas
- **AND** un proceso concurrente de vencimiento no libera sus reservas

### Requirement: Idempotencia de creación y pago agrupados [Entrega 2]

Cuando se reintente la creación o el pago de una Compra con la misma identidad y clave de idempotencia, el sistema MUST devolver el resultado existente sin duplicar Compra, entradas ni cargo.

#### Scenario: reintento con solicitud idéntica
- **GIVEN** existe una Compra o un pago procesado con una clave de idempotencia
- **WHEN** el cliente repite la misma operación con la misma clave y datos
- **THEN** recibe el resultado existente y no se duplica ningún efecto

#### Scenario: clave reutilizada con datos distintos
- **GIVEN** una clave de idempotencia ya se usó con otros datos de Compra o pago
- **WHEN** se presenta la clave con un payload diferente
- **THEN** la API rechaza la solicitud con 422 y no genera efectos adicionales

### Requirement: Devolución de una Compra agrupada [Entrega 2]

Cuando se solicite una devolución de una Compra, el sistema MUST aplicar las reglas vigentes de devolución y actualizar condicionalmente la Compra, sus entradas y el reintegro con una clave idempotente.

#### Scenario: devolución total de una Compra elegible
- **GIVEN** todas las entradas de la Compra están en estado `VALIDA`, cumplen el plazo de devolución de Entrega 1 y ninguna fue usada
- **WHEN** se solicita devolver la Compra completa con una clave de idempotencia
- **THEN** se invalidan todas sus entradas como una unidad
- **AND** se procesa un único reintegro por el total pagado
- **AND** una repetición con la misma clave no duplica el reintegro

#### Scenario: devolución total con una entrada ya usada
- **GIVEN** al menos una entrada de la Compra fue marcada `USADA`
- **WHEN** se solicita devolver la Compra completa
- **THEN** la API responde 409 y no cambia entradas ni pago

#### Scenario: devolución individual en Compra de una entrada
- **GIVEN** la Compra contiene una sola entrada `VALIDA` y cumple el plazo de devolución de Entrega 1
- **WHEN** se solicita devolver esa entrada con una clave de idempotencia
- **THEN** se procesa el reintegro del pago único y la entrada pasa por el flujo de devolución existente

#### Scenario: devolución individual parcial no soportada por la pasarela vigente
- **GIVEN** la Compra tiene más de una entrada y la pasarela configurada no soporta reintegros por monto
- **WHEN** se solicita devolver solo una de sus entradas
- **THEN** la API responde 409 con un mensaje claro de que puede devolverse la Compra completa
- **AND** no cambia entradas ni pago ni crea una solicitud pendiente de revisión
- **AND** no intenta reintegrar el total del pago agrupado
- **AND** cada entrada conserva su precio individual para habilitar devoluciones por entrada en un change futuro

### Requirement: Cancelación de Compra por cancelación del evento [Entrega 2]

Cuando `msvc-entrada` procese la cancelación coordinada del evento, el sistema MUST cerrar la fila, invalidar las Compras pendientes y liberar sus reservas y cupo sin emitir entradas ni iniciar ventas.

#### Scenario: se cancela una Compra aún pendiente
- **GIVEN** una Compra reservada y no pagada pertenece al evento que se está cancelando
- **WHEN** `POST /api/admin/entradas/eventos/{idEvento}/cancelacion` procesa la cancelación
- **THEN** la Compra queda cancelada y sus entradas reservadas quedan invalidadas
- **AND** se liberan sus asientos y se devuelve su cupo a la tanda
- **AND** no se emiten entradas ni se intenta un reintegro de un pago no cobrado
- **AND** la fila queda `CERRADA` con causa `CANCELACION` y no habilita venta libre

#### Scenario: cancelación reintegra una Compra pagada una sola vez
- **GIVEN** una Compra pagada agrupa varias entradas del evento que se cancela
- **WHEN** la cancelación procesa sus entradas y pagos
- **THEN** invalida todas las entradas de la Compra bajo la política de cancelación de Entrega 1
- **AND** procesa un único reintegro por el total de la Compra, no un reintegro por cada entrada asociada en `RegistroPago`
- **AND** usa la misma clave de idempotencia persistida de devolución completa de Compra

#### Scenario: cancelación reintegra total aunque una entrada esté usada
- **GIVEN** una Compra pagada del evento cancelado contiene varias entradas y al menos una está `USADA`
- **WHEN** la cancelación procesa esa Compra
- **THEN** invalida todas las entradas y reintegra una sola vez el total pagado de la Compra
- **AND** el estado `USADA` no reduce ni impide el reintegro porque la cancelación es responsabilidad del organizador
- **AND** un reintento de cancelación con la misma clave no duplica el reintegro

#### Scenario: reintento de cancelación no duplica el reintegro agrupado
- **GIVEN** la primera coordinación de cancelación ya reintegró una Compra de varias entradas, pero se repite la coordinación
- **WHEN** Entrada vuelve a procesar esa Compra
- **THEN** no crea un segundo reintegro
- **AND** las entradas y la Compra conservan su estado de devolución

#### Scenario: cancelación gana frente a confirmación del pago
- **GIVEN** la cancelación del evento compite con la confirmación de pago de una Compra pendiente
- **WHEN** la actualización condicional de cancelación gana
- **THEN** la Compra no puede confirmarse después, se liberan asientos y cupo y no se emiten entradas

#### Scenario: reintento de coordinación de cancelación
- **GIVEN** Evento ya está `CANCELADO` pero Entrada no completó la coordinación anterior
- **WHEN** se invoca `POST /api/admin/eventos/{id}/cancelacion/reintentar`
- **THEN** el procesamiento cierra la fila y compensa las Compras pendientes que falten
- **AND** no repite cancelaciones ni reintegros ya completados

### Requirement: Contrato de eventos de Compra confirmada [Entrega 2]

Cuando `msvc-entrada` confirme una Compra con varias entradas, MUST emitir un único hecho agregado de compra confirmada para notificaciones y mantener los eventos individuales de entrada para el contrato idempotente de preferencias. La implementación de los consumidores de Notificaciones y Preferencias queda fuera de este change.

#### Scenario: Compra grupal confirmada
- **GIVEN** una Compra de hasta seis entradas se confirma
- **WHEN** `msvc-entrada` comunica sus hechos posteriores al commit mediante Feign fire-and-forget protegido con Resilience4j, como define la arquitectura vigente
- **THEN** envía un solo evento `CompraConfirmada` por `idCompra` a `msvc-notificaciones`, con todas sus entradas incluidas
- **AND** no introduce broker y una falla de notificaciones no revierte ni demora el resultado de la Compra
- **AND** emite un `EntradaEmitida` por cada `idEntrada` para mantener el contrato de preferencias

#### Scenario: consumidores deduplican por identificador de dominio
- **GIVEN** los consumidores de Entrega 2 reciben de nuevo los eventos
- **WHEN** Notificaciones procesa `CompraConfirmada` y Preferencias procesa los eventos `EntradaEmitida`
- **THEN** el contrato exige deduplicar el aviso por `idCompra` para no multiplicarlo por la cantidad de entradas
- **AND** el contrato de Preferencias conserva su deduplicación por `idEntrada`
- **AND** la implementación y prueba de esos consumidores se coordinan con sus responsables y no forman parte de este change

### Requirement: Trazabilidad de operaciones de Compra [Entrega 2]

Cuando se ejecute una operación crítica de Compra agrupada, el sistema MUST registrar y propagar su `correlationId`.

#### Scenario: operaciones críticas conservan correlationId
- **GIVEN** ingreso a fila, Compra, devolución o cancelación comienza con un `correlationId`
- **WHEN** la operación se registra o propaga a otro microservicio
- **THEN** sus logs y llamadas interservicio conservan el mismo `correlationId`

### Requirement: Emisión individual de entradas [Entrega 2]

Cuando una Compra se confirme, el sistema MUST conservar cada entrada como unidad individual para disponibilidad, control QR e informes de entradas vendidas.

#### Scenario: consulta y escaneo de entradas compradas en grupo
- **GIVEN** una Compra pagada con más de una entrada
- **WHEN** se consulta la disponibilidad o se escanea el QR de cualquiera de sus entradas
- **THEN** cada entrada se cuenta y valida individualmente con su propio sector, asiento y QR
