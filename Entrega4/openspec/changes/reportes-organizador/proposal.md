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

Además aparecieron cuatro condiciones que hay que dejar cerradas antes de
implementar: la capacidad física de los sectores numerados, la distinción
entre 404 y 5xx de `SectorClient`, el comportamiento de `/internal/**` en el
gateway y el orden de integración con los cambios de separación de paths por
rol.

## Qué cambia

- `msvc-entrada` expone el contrato interno
  `GET /internal/entradas/eventos/{idEvento}/reporte` con entradas emitidas,
  activas, devueltas y en reintegro por sector, además de la recaudación
  calculada desde `Entrada`, `Pago` y `RegistroPago`, agregando con `GROUP BY`.
- `msvc-evento` expone
  `GET /api/organizador/eventos/{idEvento}/reporte`, valida que el evento
  pertenezca al `X-User-Id` recibido y arma la respuesta con sus sectores,
  nombres, capacidades y estado del evento, más el contrato interno de Entrada.
- El gateway **reutiliza la ruta genérica existente**
  `/api/organizador/eventos/**` hacia `msvc-evento`. No se agrega una ruta
  nueva; solo se agregan casos de prueba de rol para el path del reporte y se
  aclara el comportamiento de `/internal/**` (401 sin token, 403 con token).
- Se corrige en `msvc-ubicacion`, en un commit propio y con test, el contrato
  de sectores `NUMERADO`: `capacidadMax` se calcula en el servidor como
  `filas * asientosPorFila` y no se acepta del request; `filas` y
  `asientosPorFila` pasan a ser obligatorios en ese tipo. Hoy el mapper ya
  calcula la capacidad, pero `SectorRequest` todavía admite `capacidadMax`
  cuando coincide con el producto.
- Se unifica el contrato de ventas usado hoy por `cantidadVendida()` con el
  nuevo contrato interno, eliminando el path muerto. El orden por relevancia
  usa las entradas **activas**, no las emitidas históricas, y pasa a invocar el
  reporte completo por evento.
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

### Conteo y dinero

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
- Un evento cancelado muestra el snapshot final y **solo descuenta reembolsos
  completados**. Con el mock en `FALLA` los pagos quedan
  `REINTEGRO_PENDIENTE` y la recaudación neta sigue completa.
- La combinación `DEVUELTA + REINTEGRO_PENDIENTE` no se alcanza por el flujo
  real del mock: con `FALLA` la entrada queda `EN_REINTEGRO`. El test que la
  cubre es una prueba de la regla con datos armados a mano y debe decirlo en su
  nombre y comentario.
- La agregación se implementa con `GROUP BY (idSector, estado)` para entradas y
  `GROUP BY (idSector, estadoPago)` para dinero, no con una consulta por estado.

### Capacidad

- La capacidad es la **física del sector**, calculada por `msvc-ubicacion`:
  en `NUMERADO` es `filas * asientosPorFila` y se calcula en el servidor; en
  `NO_NUMERADO` es `capacidadMax` informada.
- `EventoSector` no guarda capacidad: solo `precio`. No existe una capacidad
  propia en Evento que contradiga a `SectorResponse.capacidadMax`.
- `SectorClient` responde 404 → el sector fue borrado y no hay FK: se incluye
  con `capacidad = 0`, `nombreSector = null`, `porcentajeOcupacion = 0.00` y
  sus cifras intactas.
- Error de red o 5xx de `SectorClient` → 502, sin cifras parciales.

### Errores y seguridad

- Evento ajeno → 403; evento inexistente → 404. Es la convención vigente de
  `identidad-servicios`, no un cambio puntual del reporte.
- `idEvento` no numérico → 400.
- `X-User-Id` ausente o no numérico → 403, nunca 500 por
  `NumberFormatException`.
- `/internal/**` por el gateway → 401 sin token y 403 con token (`denyAll`).

### Pruebas y cierre

- No se modifica ningún test existente para que pase. Si alguno queda obsoleto
  por el cambio de contrato de `cantidadVendida()`, se reemplaza en commit
  separado y se registra en `tasks.md`. `NoDobleVentaTest` queda intacto.
- La verificación de integración con `docker-compose` se mueve a EP-21. Este
  change se archiva al pasar los tests unitarios y de gateway, con una tarea de
  seguimiento en EP-21 que lo referencie.
- La implementación no arranca hasta que: (a) el smoke test de login real esté
  verde y (b) esté integrado el commit de `msvc-evento` de separación de paths
  por rol, que toca `organizador-eventos-route` y la eliminación de
  `validarRol`.

## Capacidades

- Nueva capacidad: `reportes-organizador`.
- Impacto en `gateway-ruteo`: confirma que la ruta genérica ya alcanza el
  reporte y refuerza que el contrato interno de Entrada no es público.
- Impacto en `gateway-seguridad`: agrega escenarios de rol para el reporte y
  la regla `denyAll` de `/internal/**`; la identidad ya está cubierta en
  `GatewayErrorHandlingTest`.
- Impacto menor en `identidad-servicios`: agrega los escenarios de pertenencia
  e identidad inválida aplicados al reporte, sin cambiar la regla ya definida.

## Alcance

- Corrección de `SectorRequest`/validación en `msvc-ubicacion`, en commit
  propio y con test.
- Repositorios, service, DTOs y controller de `msvc-entrada`.
- DTOs, service y controller de `msvc-evento`.
- Cliente Feign `EntradaClient` y ajuste de `cantidadVendida()`.
- Casos de prueba de rol y de `/internal/**` en el gateway.
- Tests unitarios y de gateway; verificación de integración diferida.
- Documentación OpenSpec y `openspec/config.yaml`.

## Fuera del alcance

- Dockerfiles, JMeter y el levantamiento de servicios en `docker-compose`
  (EP-21). La verificación de integración de este change queda como tarea de
  seguimiento de EP-21 con referencia a este change.
- Trazabilidad global y log centralizado (EP-20).
- Resiliencia real con timeouts, retries o circuit breaker (EP-13).
- Notificaciones por reporte o cancelación (EP-14).
- Histórico diario o series temporales de reportes.
- Endpoint interno liviano de solo activas para `cantidadVendida()`: se
  considerará solo si la medición muestra lentitud.
- Cambiar ADR-0001, ADR-0002, ADR-0003 ni el contrato del JWT.
- Exponer `/internal/**` en el gateway.
- Modificar tests existentes para que pasen. No se toca `NoDobleVentaTest`.
- Hacer push sin autorización explícita.

## Impacto

Se agrega un GET bajo un prefijo ya autorizado para `ORGANIZADOR`, sin cambiar
las rutas ni los DTOs públicos existentes. Un organizador que no sea dueño del
evento recibe 403 sin cifras. La recaudación se calcula con el importe
realmente cobrado en `RegistroPago.montoTotal`, no con el precio actual del
evento, y la ocupación usa la capacidad física del sector.

## Estado

Change aprobado con los ajustes del 2026-11-22. Queda listo para implementación
bajo las dos condiciones de orden de ejecución. No se modificó código de
producción ni tests existentes.
