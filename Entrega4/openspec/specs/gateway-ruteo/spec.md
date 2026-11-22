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
- Y conserva el path, método, query parameters, body y headers de la solicitud,
  con la identidad definida en `gateway-seguridad`

#### Scenario: devoluciones se enrutan bajo entradas

- Dado que `msvc-entrada` está registrado en Eureka
- Cuando el cliente solicita o resuelve una devolución bajo
  `/api/entradas/{id}/devolucion`
- Entonces el gateway enruta la solicitud a `msvc-entrada`

#### Scenario: artistas conservan un único path

- Dado que las consultas públicas de artistas se hacen bajo `/api/artistas/**`
- Y las escrituras administrativas se hacen bajo `/api/admin/artistas/**`
- Cuando el gateway resuelve cualquiera de esos prefijos
- Entonces enruta a `msvc-evento`
- Y no existe una ruta pública `/api/participantes/**`

#### Scenario: disponibilidad pública bajo eventos

- Dado que el cliente pide `GET /api/eventos/{idEvento}/disponibilidad`
- Cuando el gateway evalúa la ruta
- Entonces la permite sin token por estar bajo `GET /api/eventos/**`
- Y `msvc-evento` delega en el contrato interno
  `/internal/entradas/disponibilidad` de `msvc-entrada`

### Requirement: No exponer rutas fuera del contrato [Entrega 1]

Cuando el cliente solicite un path sin ruta pública configurada, el gateway
DEBE responder 404 sin seleccionar un servicio destino.

#### Scenario: registros de pago no tienen ruta pública

- Dado que no existe una ruta pública para `/api/registros-pago/**`
- Cuando el cliente llama ese path a través del gateway
- Entonces el gateway responde 404

#### Scenario: contratos internos no tienen ruta pública

- Dado que `/internal/entradas/disponibilidad` es un contrato entre servicios
- Cuando un cliente intenta llamarlo por el gateway
- Entonces la cadena de seguridad lo deniega sin enrutarlo a `msvc-entrada`

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

> La propagación no autenticada de `X-User-Id` y `X-User-Role` de Entrega 1
> quedó superseded. La identidad vigente se rige por `gateway-seguridad` e
> `identidad-servicios`; la versión histórica se conserva en el change
> archivado `2026-11-20-gateway-entrega1`.

### Requirement: Ruteo del reporte del organizador [Entrega 2]

El gateway DEBE enrutar el reporte de un organizador hacia `msvc-evento`
mediante el service ID registrado en Eureka, reutilizando la ruta genérica
existente.

#### Scenario: reporte del organizador

- Dado que `msvc-evento` está registrado en Eureka
- Cuando un organizador llama
  `GET /api/organizador/eventos/{idEvento}/reporte`
- Entonces el gateway enruta la solicitud a `lb://msvc-evento` mediante
  `organizador-eventos-route`
- Y conserva el path, el método y el `X-User-Id` derivado del JWT

#### Scenario: reutilización de la ruta genérica

- Dado que ya existe `organizador-eventos-route` para
  `/api/organizador/eventos/**` en `application.yml` y en la configuración
  central de `msvc-gateway`
- Cuando se implementa el reporte
- Entonces **no** se agrega `organizador-reportes-route` ni otra ruta nueva
- Y `GatewayRoleMatrixTest.cadaRutaDelGatewayTieneReglaExplicita` sigue
  protegiendo el sistema ante rutas sin regla de rol

#### Scenario: contrato interno de reporte no es público

- Dado que `msvc-entrada` expone
  `/internal/entradas/eventos/{idEvento}/reporte`
- Cuando un cliente intenta llamarlo por el gateway
- Entonces la cadena de seguridad lo deniega sin enrutarlo: 401 sin token y
  403 con token por `denyAll`
- Y no existe una ruta pública equivalente bajo `/api/entradas/**`
