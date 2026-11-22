# Propuesta: reportes de eventos para organizadores (Entrega 2)

## Por qué

La historia US-12 y la épica EP-18 exigen que un organizador consulte entradas
vendidas y devueltas por sector, porcentaje de ocupación y recaudación neta de
sus propios eventos.

Hoy esa funcionalidad no existe: `OrganizadorEventoController` solo expone
escrituras, `msvc-entrada` no publica el detalle de ventas y `EntradaClient`
declara un contrato `/eventos/{idEvento}/ventas` que no existe. El gateway ya
tiene el prefijo `/api/organizador/eventos/**` con rol `ORGANIZADOR`, pero no
hay escenarios que cubran el reporte.

## Qué cambia

- `msvc-entrada` expone el contrato interno
  `GET /internal/entradas/eventos/{idEvento}/reporte` con entradas emitidas,
  activas, devueltas y en reintegro por sector, además de la recaudación
  calculada desde `Entrada`, `Pago` y `RegistroPago`.
- `msvc-evento` expone
  `GET /api/organizador/eventos/{idEvento}/reporte`, valida que el evento
  pertenezca al `X-User-Id` recibido y arma la respuesta con sus sectores,
  nombres, capacidades y estado del evento, más el contrato interno de Entrada.
- El gateway **reutiliza la ruta genérica existente**
  `/api/organizador/eventos/**` hacia `msvc-evento`. No se agrega una ruta
  nueva; solo se agregan casos de prueba de rol para el path del reporte.
- Se unifica el contrato de ventas usado hoy por `cantidadVendida()` con el
  nuevo contrato interno, eliminando el path muerto. El orden por relevancia
  usa las entradas **activas**, no las emitidas históricas.
- Se publican las specs vivas y se actualizan `gateway-ruteo`,
  `gateway-seguridad`, `identidad-servicios` y `openspec/config.yaml`.

## Contratos propuestos

### Público

`GET /api/organizador/eventos/{idEvento}/reporte`

```json
{
  "idEvento": 10,
  "estadoEvento": "PUBLICADO",
  "porSector": [
    {
      "idSector": 1,
      "nombreSector": "Platea",
      "capacidad": 200,
      "entradasEmitidas": 127,
      "entradasActivas": 120,
      "entradasDevueltas": 5,
      "enReintegro": 2,
      "porcentajeOcupacion": 60.00,
      "recaudacionBruta": 127000.00,
      "recaudacionDevuelta": 5000.00,
      "recaudacionNeta": 122000.00
    }
  ],
  "totales": {
    "entradasEmitidas": 127,
    "entradasActivas": 120,
    "entradasDevueltas": 5,
    "enReintegro": 2,
    "porcentajeOcupacion": 60.00,
    "recaudacionBruta": 127000.00,
    "recaudacionDevuelta": 5000.00,
    "recaudacionNeta": 122000.00
  }
}
```

### Interno

`GET /internal/entradas/eventos/{idEvento}/reporte`

Igual que el público, pero sin `estadoEvento`, `nombreSector`, `capacidad` ni
`porcentajeOcupacion`, porque esos datos los aporta `msvc-evento`.

```json
{
  "idEvento": 10,
  "porSector": [
    {
      "idSector": 1,
      "entradasEmitidas": 127,
      "entradasActivas": 120,
      "entradasDevueltas": 5,
      "enReintegro": 2,
      "recaudacionBruta": 127000.00,
      "recaudacionDevuelta": 5000.00,
      "recaudacionNeta": 122000.00
    }
  ],
  "totales": {
    "entradasEmitidas": 127,
    "entradasActivas": 120,
    "entradasDevueltas": 5,
    "enReintegro": 2,
    "recaudacionBruta": 127000.00,
    "recaudacionDevuelta": 5000.00,
    "recaudacionNeta": 122000.00
  }
}
```

## Decisiones confirmadas

- `entradasEmitidas` es el histórico:
  `VALIDA + USADA + EN_REINTEGRO + DEVUELTA`.
- `entradasActivas` son las vigentes: `VALIDA + USADA`. La ocupación y el
  orden por relevancia usan este campo, no el histórico.
- `RESERVADA` no cuenta en ningún campo.
- `EN_REINTEGRO` se informa separado y no cuenta como activa ni ocupa.
- `DEVUELTA` cuenta como devuelta, pero el dinero se descuenta recién cuando
  el pago está `REEMBOLSADO`, para no descontar dos veces.
- `REINTEGRO_PENDIENTE` sigue contando en recaudación bruta y neta.
- `RegistroPago.montoTotal` es por entrada, con `Pago.idEntrada` único y clave
  `(idEntrada, idPago)`. Varias entradas aprobadas suman varios registros; no
  se duplica un pago compartido.
- Capacidad `null` o 0 produce `porcentajeOcupacion: 0.00`.
- `totales.porcentajeOcupacion` se calcula sobre la suma de capacidades; si la
  suma es 0, devuelve `0.00`.
- Un sector con ventas pero sin capacidad configurada se incluye con
  `capacidad: 0` y `porcentajeOcupacion: 0.00`.
- El reporte es un snapshot actual, sin filtros de fecha ni histórico.
- Evento ajeno responde 403; evento inexistente responde 404.

## Capacidades

- Nueva capacidad: `reportes-organizador`.
- Impacto en `gateway-ruteo`: confirma que la ruta genérica ya alcanza el
  reporte y refuerza que el contrato interno de Entrada no es público.
- Impacto en `gateway-seguridad`: agrega escenarios de rol para el reporte; la
  identidad ya está cubierta en `GatewayErrorHandlingTest`.
- Impacto menor en `identidad-servicios`: agrega el escenario de pertenencia
  aplicado al reporte, sin cambiar la regla ya definida.

## Alcance

- Repositorios, service, DTOs y controller de `msvc-entrada`.
- DTOs, service y controller de `msvc-evento`.
- Cliente Feign `EntradaClient` y ajuste de `cantidadVendida()`.
- Casos de prueba de rol del gateway sobre el path del reporte.
- Tests unitarios y de integración ejecutados contra `docker-compose`.
- Documentación OpenSpec y `openspec/config.yaml`.

## Fuera del alcance

- Dockerfiles, JMeter y el levantamiento de servicios en `docker-compose`
  (EP-21). La verificación de integración de este change se ejecutará contra
  ese entorno cuando esté disponible y queda registrada como dependencia.
- Trazabilidad global y log centralizado (EP-20).
- Resiliencia real con timeouts, retries o circuit breaker (EP-13).
- Notificaciones por reporte o cancelación (EP-14).
- Histórico diario o series temporales de reportes.
- Cambiar ADR-0001, ADR-0003 ni el contrato del JWT.
- Exponer `/internal/**` en el gateway.
- Modificar tests existentes para que pasen. No se toca `NoDobleVentaTest`.
- Hacer push sin autorización explícita.

## Impacto

Se agrega un GET bajo un prefijo ya autorizado para `ORGANIZADOR`, sin cambiar
las rutas ni los DTOs públicos existentes. Un organizador que no sea dueño del
evento recibe 403 sin cifras. La recaudación se calcula con el importe
realmente cobrado en `RegistroPago.montoTotal`, no con el precio actual del
evento.

## Estado

Change aprobado con ajustes el 2026-11-21. Queda listo para implementación,
condicionado a que el smoke test de login real esté verde. No se modificó
código de producción ni tests existentes.
