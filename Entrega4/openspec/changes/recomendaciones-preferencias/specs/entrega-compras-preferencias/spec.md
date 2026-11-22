# Spec Delta

## Purpose

Entrega de forma asíncrona y confiable la notificación de compra aprobada hacia preferencias, sin afectar la compra del cliente y tolerando caídas del destino, reintentos del worker y ejecución con varias instancias.

## ADDED Requirements

### Requirement: Persistencia atómica del pendiente [Entrega 2]

Cuando se aprueba un pago, el sistema DEBE registrar el aviso pendiente en la misma transacción que la entrada y el pago, de modo que ambos existan o ninguno.

#### Scenario: pago aprobado

- **WHEN** un pago pasa a aprobado y se emite la entrada
- **THEN** queda registrado un aviso pendiente para esa entrada
- **AND** el cliente recibe la confirmación sin esperar a preferencias

#### Scenario: falla la transacción

- **WHEN** la transacción del pago se revierte
- **THEN** no queda ningún aviso pendiente

### Requirement: Solo compras aprobadas [Entrega 2]

El sistema NO DEBE registrar avisos para pagos rechazados, vencidos ni fallidos.

#### Scenario: pago rechazado

- **WHEN** el pago es rechazado o la reserva vence
- **THEN** la entrada se libera
- **AND** no se crea ningún aviso pendiente

### Requirement: Reintentos con backoff [Entrega 2]

Si preferencias no responde, el sistema DEBE realizar 5 intentos reales en total (no 5 reintentos adicionales a un intento inicial), con esperas de 10 segundos, 30 segundos, 1 minuto y 5 minutos entre cada uno, configurables sin tocar el código.

#### Scenario: falla transitoria

- **WHEN** la llamada a preferencias falla por error real
- **THEN** se incrementa el contador de intentos
- **Y** se programa el siguiente intento según el backoff configurado

### Requirement: Circuito abierto sin consumo de intento [Entrega 2]

Si el circuito está abierto, el sistema NO DEBE incrementar el contador de intentos y DEBE reprogramar el aviso con una espera derivada del tiempo de apertura del circuito más cinco segundos.

#### Scenario: rechazo por circuito abierto

- **WHEN** la llamada es rechazada porque el circuito está abierto
- **THEN** el contador de intentos permanece igual
- **AND** el siguiente intento se programa con la espera derivada de la configuración del circuito

### Requirement: Agotamiento de reintentos [Entrega 2]

Cuando se agota el quinto intento real, el sistema DEBE marcar el aviso como fallido y conservarlo para reprocesamiento manual.

#### Scenario: quinto fallo real

- **WHEN** falla el quinto intento real
- **THEN** el aviso pasa al estado fallido
- **Y** el registro no se elimina

### Requirement: Procesamiento exclusivo con varias instancias [Entrega 2]

El sistema DEBE garantizar que dos instancias activas no procesen el mismo aviso, mediante una reclamación atómica de cada registro.

#### Scenario: dos instancias consultan pendientes

- **WHEN** dos instancias buscan avisos para procesar al mismo tiempo
- **THEN** cada una reclama un registro distinto
- **Y** ninguna procesa un registro ya reclamado

#### Scenario: registro abandonado

- **WHEN** un registro queda en procesamiento más de cinco minutos
- **THEN** el sistema lo reclama nuevamente para otra instancia

### Requirement: Actualización atómica del resultado [Entrega 2]

El sistema DEBE actualizar el contador de intentos, la fecha del siguiente intento y el estado en una sola transacción, condicionada a que el registro siga en procesamiento.

#### Scenario: resultado de un intento

- **WHEN** se registra el resultado de una llamada
- **THEN** intentos, fecha y estado se actualizan juntos
- **AND** si el registro ya no está en procesamiento, la actualización no produce cambios

### Requirement: Trazabilidad del aviso [Entrega 2]

El sistema DEBE conservar el identificador de correlación del pago y propagarlo al hilo asíncrono y a la llamada hacia preferencias.

#### Scenario: seguimiento de extremo a extremo

- **WHEN** se procesa un aviso pendiente
- **THEN** los registros del worker incluyen el identificador de correlación del pago
- **Y** la llamada a preferencias lo envía como header

### Requirement: Resiliencia de la llamada [Entrega 2]

La llamada hacia preferencias DEBE usar un circuito breaker con ventana temporal de 120 segundos, mínimo tres llamadas y umbral de fallo del 50 por ciento, con transición automática a semi abierto y un TimeLimiter de 2 segundos. El cliente HTTP DEBE configurarse con `connect-timeout: 500` y `read-timeout: 1200` milisegundos, sin reintento interno en la librería de resiliencia.

#### Scenario: patrón de fallo

- **WHEN** ocurren tres llamadas en la ventana y dos fallan
- **THEN** el circuito se abre
- **Y** pasa a semi abierto automáticamente tras el tiempo configurado

#### Scenario: tiempo límite del cliente

- **WHEN** se configura el cliente HTTP con 500 ms de conexión y 1200 ms de lectura
- **THEN** la suma es 1700 ms, menor al TimeLimiter de 2 segundos
- **Y** no se configura reintento interno en la librería de resiliencia
