# Propuesta: reportes de eventos para organizadores (Entrega 2)

## Por qué

La historia US-12 y la épica EP-18 exigen que un organizador consulte entradas
vendidas y devueltas por sector, porcentaje de ocupación y recaudación neta de
sus propios eventos.

Hoy esa funcionalidad no existe: `OrganizadorEventoController` solo expone
escrituras, `msvc-entrada` no publica el detalle de ventas y `EntradaClient`
declara un contrato `/eventos/{idEvento}/ventas` que no existe. El gateway ya
tiene el prefijo `/api/organizador/eventos/**` con rol `ORGANIZADOR`, pero no
hay una ruta explícita ni escenarios que cubran el reporte.

## Qué cambia

- `msvc-entrada` expone el contrato interno
  `GET /internal/entradas/eventos/{idEvento}/reporte` con conteos por sector y
  recaudación calculada desde `Entrada`, `Pago` y `RegistroPago`.
- `msvc-evento` expone
  `GET /api/organizador/eventos/{idEvento}/reporte`, valida que el evento
  pertenezca al `X-User-Id` recibido y arma la respuesta con sus sectores,
  nombres y capacidades más el contrato interno de Entrada.
- El gateway agrega una ruta explícita hacia `msvc-evento` y casos de prueba
  de autorización para el nuevo path.
- Se unifica el contrato de ventas usado hoy por `cantidadVendida()` con el
  nuevo contrato interno, eliminando el path muerto.
- Se publican las specs vivas y se actualizan `gateway-ruteo`,
  `gateway-seguridad`, `identidad-servicios` y `openspec/config.yaml`.

## Contratos propuestos

### Público

`GET /api/organizador/eventos/{idEvento}/reporte`

```json
{
  "idEvento": 10,
  "porSector": [
    {
      "idSector": 1,
      "nombreSector": "Platea",
      "capacidad": 200,
      "entradasVendidas": 120,
      "entradasDevueltas": 5,
      "enReintegro": 2,
      "porcentajeOcupacion": 60.00,
      "recaudacionBruta": 125000.00,
      "recaudacionDevuelta": 5000.00,
      "recaudacionNeta": 120000.00
    }
  ],
  "totales": {
    "entradasVendidas": 120,
    "entradasDevueltas": 5,
    "enReintegro": 2,
    "porcentajeOcupacion": 60.00,
    "recaudacionBruta": 125000.00,
    "recaudacionDevuelta": 5000.00,
    "recaudacionNeta": 120000.00
  }
}
```

### Interno

`GET /internal/entradas/eventos/{idEvento}/reporte`

Igual que el público, pero sin `nombreSector`, `capacidad` ni
`porcentajeOcupacion`, porque esos datos los aporta `msvc-evento`.

## Decisiones confirmadas

- El reporte es un **snapshot actual**, sin filtros de fecha ni histórico.
- `RESERVADA` no cuenta; `VALIDA` y `USADA` cuentan como vendidas.
- `EN_REINTEGRO` se informa separado y no cuenta como ocupada.
- `DEVUELTA` cuenta como devuelta, pero el dinero se descuenta recién cuando
  el pago está `REEMBOLSADO`, para no descontar dos veces.
- `REINTEGRO_PENDIENTE` sigue contando en recaudación neta.
- Capacidad `null` o 0 produce `porcentajeOcupacion: 0.00`.
- `totales.porcentajeOcupacion` se calcula sobre la suma de capacidades; si la
  suma es 0, devuelve `0.00`.
- Un sector con ventas pero sin capacidad configurada se incluye con
  `capacidad: 0` y `porcentajeOcupacion: 0.00`.

## Capacidades

- Nueva capacidad: `reportes-organizador`.
- Impacto en `gateway-ruteo`: agrega el enrutamiento del reporte y refuerza que
  el contrato interno de Entrada no es público.
- Impacto en `gateway-seguridad`: agrega escenarios de rol, identidad y
  descarte de `X-User-Role`.
- Impacto menor en `identidad-servicios`: agrega el escenario de pertenencia
  aplicado al reporte, sin cambiar la regla ya definida.

## Alcance

- Repositorios, service, DTOs y controller de `msvc-entrada`.
- DTOs, service y controller de `msvc-evento`.
- Cliente Feign `EntradaClient` y ajuste de `cantidadVendida()`.
- Rutas YAML del gateway, matriz de roles y tests de `GatewayRoleMatrixTest`.
- Tests unitarios y de integración con Testcontainers.
- Documentación OpenSpec y `openspec/config.yaml`.

## Fuera del alcance

- docker-compose, Dockerfiles, JMeter y E2E (EP-21).
- Trazabilidad/correlationId (Fase 2, `trazabilidad-gateway`).
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

Change creado el 2026-11-21 con las tres decisiones propuestas confirmadas por
el grupo. Pendiente de implementación; no se modificó código de producción ni
tests existentes.
