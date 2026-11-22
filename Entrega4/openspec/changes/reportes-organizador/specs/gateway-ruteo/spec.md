# Requisitos: ruteo del gateway

## ADDED Requirements

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
