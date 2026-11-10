# Requisitos: ruteo del gateway

## ADDED Requirements

### Requirement: Enrutamiento de APIs públicas [Entrega 1]

Cuando el gateway reciba una solicitud bajo un path público configurado, el
sistema DEBE enrutarla al servicio dueño de esa API mediante el service ID
registrado en Eureka.

#### Scenario: solicitudes se enrutan al servicio correspondiente

- Dado que Eureka tiene registrada una instancia saludable del servicio destino
- Cuando el cliente llama un path público configurado en el gateway
- Entonces el gateway reenvía la solicitud a ese servicio
- Y conserva el path, método, query parameters, body y headers de la solicitud

#### Scenario: devoluciones se enrutan bajo entradas

- Dado que `msvc-entrada` está registrado en Eureka
- Cuando el cliente solicita o resuelve una devolución bajo
  `/api/entradas/{id}/devolucion`
- Entonces el gateway enruta la solicitud a `msvc-entrada`

### Requirement: No exponer rutas fuera del contrato [Entrega 1]

Cuando el cliente solicite un path sin ruta pública configurada, el gateway
DEBE responder 404 sin seleccionar un servicio destino.

#### Scenario: registros de pago no tienen ruta pública

- Dado que no existe una ruta pública para `/api/registros-pago/**`
- Cuando el cliente llama ese path a través del gateway
- Entonces el gateway responde 404

### Requirement: Propagación de headers de usuario en E1 [Entrega 1]

Cuando una solicitud incluya `X-User-Id` o `X-User-Role`, el gateway DEBE
propagar esos headers al servicio destino sin tratarlos como credenciales
autenticadas.

#### Scenario: headers usados por los servicios llegan al destino

- Dado que la solicitud incluye `X-User-Id` y `X-User-Role`
- Cuando el gateway la reenvía al servicio destino
- Entonces ambos headers llegan sin alteración

### Requirement: Formato uniforme de errores del gateway [Entrega 1]

Cuando el gateway rechace una ruta inexistente o no pueda encontrar una
instancia destino, el sistema DEBE responder con JSON que incluya `status`,
`error`, `message` y `path`, sin incluir stack traces.

#### Scenario: path desconocido devuelve error JSON uniforme

- Dado que el cliente llama un path que no tiene ruta configurada
- Cuando el gateway resuelve la solicitud
- Entonces responde con status HTTP 404
- Y el body contiene `status`, `error`, `message` y `path`

#### Scenario: servicio destino sin instancias devuelve error JSON uniforme

- Dado que existe una ruta configurada pero Eureka no ofrece instancias
- Cuando el gateway intenta enrutar la solicitud
- Entonces responde con status HTTP 503
- Y el body contiene `status`, `error`, `message` y `path`

#### Scenario: conservar errores de negocio del destino

- Dado que el servicio destino devuelve una respuesta HTTP de error
- Cuando el gateway reenvía la respuesta al cliente
- Entonces conserva el status y body enviados por el servicio destino

### Requirement: Seguridad real del gateway [Entrega 2]

Cuando se implemente la seguridad de Entrega 2, el gateway DEBE validar JWT y
los servicios DEBEN rechazar acceso externo que no atraviese el gateway.

#### Scenario: solicitud sin token en Entrega 2

- Dado que la seguridad JWT está habilitada
- Cuando el cliente llama una ruta protegida sin token válido
- Entonces el gateway responde 401

> Este requisito se documenta como futuro y queda fuera de
> `gateway-entrega1`, conforme a EP-12.
