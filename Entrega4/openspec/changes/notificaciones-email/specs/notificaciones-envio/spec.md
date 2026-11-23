# Spec Delta

## Purpose

Definir cómo `msvc-notificaciones` recibe solicitudes de notificación, las envía por email de forma asíncrona y confiable, y qué garantiza ante reintentos, caídas del SMTP y varias instancias.

## ADDED Requirements

### Requirement: Solicitud idempotente de notificación [Entrega 2]
Cuando otro microservicio solicita una notificación, el sistema deberá (SHALL) registrarla como `PENDIENTE` sin esperar al envío y responder `202` con la notificación. El sistema deberá (SHALL) identificar cada solicitud por su `claveIdempotencia` y no deberá (SHALL NOT) crear ni enviar una segunda notificación con la misma clave.

#### Scenario: Solicitud nueva
- **WHEN** se envía `POST /internal/notificaciones` con clave, `idUsuario`, `tipo` y los datos obligatorios del tipo
- **THEN** responde 202 con la notificación en estado `PENDIENTE`
- **AND** el envío ocurre después, en otro hilo

#### Scenario: Reintento con la misma clave
- **WHEN** se envía una solicitud con una `claveIdempotencia` ya registrada
- **THEN** responde 202 con la notificación existente
- **AND** no se crea ni se envía otra

#### Scenario: Solicitudes simultáneas con la misma clave
- **WHEN** dos solicitudes con la misma clave llegan a la vez
- **THEN** queda una sola notificación
- **AND** ambas respuestas devuelven la misma

#### Scenario: Faltan datos obligatorios del tipo
- **WHEN** la solicitud no trae alguno de los datos obligatorios de su tipo
- **THEN** responde 400 indicando los datos faltantes y no registra la notificación

#### Scenario: Datos inválidos
- **WHEN** falta la clave, el `idUsuario` o el `tipo`, o el `destinatario` no es un email válido
- **THEN** responde 400 indicando cada campo inválido

### Requirement: Envío asíncrono por email [Entrega 2]
El sistema deberá (SHALL) enviar el email por SMTP en un hilo de un pool propio, con asunto y cuerpo armados según el tipo de notificación, sin bloquear la respuesta de la solicitud. Al enviar, el sistema deberá (SHALL) marcar la notificación como `ENVIADA` con su fecha de envío.

#### Scenario: Envío correcto
- **WHEN** el servidor SMTP acepta el mensaje
- **THEN** la notificación queda `ENVIADA` con `fechaEnvio` y un intento registrado

#### Scenario: Plantilla por tipo
- **WHEN** se envía una notificación `COMPRA_CONFIRMADA`
- **THEN** el asunto y el cuerpo incluyen el evento, la fecha y la entrada recibidos en los datos

#### Scenario: Pool saturado
- **WHEN** el pool de hilos rechaza el envío
- **THEN** la solicitud igualmente responde 202
- **AND** la notificación sigue `PENDIENTE` y la envía el worker de reintentos

### Requirement: Un solo envío por notificación [Entrega 2]
El sistema deberá (SHALL) reclamar cada notificación de forma atómica antes de enviarla, de modo que un solo hilo o instancia la envíe, y no deberá (SHALL NOT) volver a enviar una ya `ENVIADA`.

#### Scenario: Dos instancias eligen la misma notificación
- **WHEN** dos instancias intentan enviar la misma notificación a la vez
- **THEN** solo una la reclama y la envía

#### Scenario: Notificación ya enviada
- **WHEN** se intenta procesar una notificación `ENVIADA`
- **THEN** no se envía otro email

### Requirement: Reintentos con backoff [Entrega 2]
Si el envío falla, el sistema deberá (SHALL) reintentarlo hasta cinco veces con esperas de 10 segundos, 30 segundos, 1 minuto y 5 minutos, configurables sin tocar el código, y deberá (SHALL) conservar la notificación como `FALLIDA` al agotar los intentos.

#### Scenario: Falla transitoria
- **WHEN** el envío falla por un error real
- **THEN** la notificación queda `FALLIDA` con un intento más, el motivo en `ultimoError` y el próximo intento programado según el backoff

#### Scenario: Reintento exitoso
- **WHEN** un reintento es aceptado por el servidor SMTP
- **THEN** la notificación pasa a `ENVIADA`

#### Scenario: Intentos agotados
- **WHEN** falla el quinto intento real
- **THEN** la notificación queda `FALLIDA` sin próximo intento y no se vuelve a elegir

#### Scenario: Notificación trabada
- **WHEN** una notificación `ENVIANDO` no se actualiza hace más del tiempo de recuperación
- **THEN** vuelve a ser elegible para enviarse

### Requirement: Resiliencia del envío [Entrega 2]
El sistema deberá (SHALL) proteger el envío con un circuito y un tiempo máximo. Si el circuito está abierto, el sistema no deberá (SHALL NOT) contar el rechazo como intento y deberá (SHALL) reprogramar la notificación como `PENDIENTE`.

#### Scenario: Circuito abierto
- **WHEN** el envío es rechazado porque el circuito está abierto
- **THEN** el contador de intentos no cambia
- **AND** la notificación queda `PENDIENTE` con el próximo intento posterior a la espera configurada

#### Scenario: Tiempo máximo excedido
- **WHEN** el SMTP no responde dentro del tiempo máximo
- **THEN** se cuenta como un intento fallido y se aplica el backoff

### Requirement: Resolución del destinatario [Entrega 2]
El sistema deberá (SHALL) enviar al `destinatario` de la solicitud si viene, o consultar el email del usuario a `msvc-usuarios` si no. El sistema deberá (SHALL) descartar la notificación, sin reintentarla, si el usuario no existe o no tiene email.

#### Scenario: Destinatario en la solicitud
- **WHEN** la solicitud trae `destinatario`
- **THEN** se envía a esa dirección sin consultar a `msvc-usuarios`

#### Scenario: Destinatario consultado
- **WHEN** la solicitud no trae `destinatario` y `msvc-usuarios` devuelve el email del usuario
- **THEN** se guarda el email en la notificación y se envía a esa dirección

#### Scenario: Usuario sin email o inexistente
- **WHEN** `msvc-usuarios` responde 404 o devuelve un usuario sin email
- **THEN** la notificación queda `DESCARTADA` con el motivo en `ultimoError`
- **AND** no se reintenta

#### Scenario: msvc-usuarios no responde
- **WHEN** la consulta a `msvc-usuarios` falla por un error o timeout
- **THEN** se cuenta como un intento fallido y se aplica el backoff

### Requirement: Trazabilidad [Entrega 2]
El sistema deberá (SHALL) tomar el header `X-Correlation-Id` de cada solicitud, guardarlo en la notificación y mantenerlo en el log del envío, incluso en el hilo asíncrono y en los reintentos.

#### Scenario: Solicitud con correlationId
- **WHEN** la solicitud llega con `X-Correlation-Id`
- **THEN** la notificación guarda ese valor
- **AND** el log del envío lo incluye

#### Scenario: Solicitud sin correlationId
- **WHEN** la solicitud llega sin el header
- **THEN** el sistema genera uno y lo devuelve en la respuesta

### Requirement: Contrato interno [Entrega 2]
El sistema deberá (SHALL) exponer sus endpoints solo bajo `/internal/**` y no deberá (SHALL NOT) tener rutas públicas en el gateway.

#### Scenario: Acceso desde el gateway
- **WHEN** un cliente intenta alcanzar `/internal/notificaciones` por el gateway
- **THEN** el gateway rechaza la solicitud
