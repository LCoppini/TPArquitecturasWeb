# Requisitos: ruteo del gateway

## ADDED Requirements

### Requirement: Ruteo del reporte del organizador [Entrega 2]

El gateway DEBE enrutar el reporte de un organizador hacia `msvc-evento`
mediante el service ID registrado en Eureka.

#### Scenario: reporte del organizador

- Dado que `msvc-evento` está registrado en Eureka
- Cuando un organizador llama
  `GET /api/organizador/eventos/{idEvento}/reporte`
- Entonces el gateway enruta la solicitud a `lb://msvc-evento`
- Y conserva el path, el método y el `X-User-Id` derivado del JWT

#### Scenario: ruta explícita antes que la genérica

- Dado que existe `organizador-eventos-route` para
  `/api/organizador/eventos/**`
- Cuando se declara `organizador-reportes-route`
- Entonces la ruta del reporte se declara antes
- Y aparece tanto en `application.yml` como en la configuración central de
  `msvc-gateway`

#### Scenario: contrato interno de reporte no es público

- Dado que `msvc-entrada` expone
  `/internal/entradas/eventos/{idEvento}/reporte`
- Cuando un cliente intenta llamarlo por el gateway
- Entonces la cadena de seguridad lo deniega sin enrutarlo
- Y no existe una ruta pública equivalente bajo `/api/entradas/**`
