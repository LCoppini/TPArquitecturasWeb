# Requisitos: reportes de eventos para organizadores

## ADDED Requirements

### Requirement: Consulta del reporte por el organizador dueño [Entrega 2]

El sistema DEBE permitir a un organizador consultar el reporte de un evento
cuando el `idOrganizador` del evento coincida con el `X-User-Id` propagado por
el gateway.

#### Scenario: organizador dueño consulta su reporte

- Dado que existe un evento con `idOrganizador = 42`
- Y el JWT del solicitante tiene `idUsuario = 42` y rol `ORGANIZADOR`
- Cuando llama `GET /api/organizador/eventos/{idEvento}/reporte`
- Entonces responde 200
- Y el body contiene `idEvento`, `porSector` y `totales`

#### Scenario: otro organizador intenta consultarlo

- Dado que el evento pertenece al organizador `42`
- Y el solicitante es el organizador `99`
- Cuando llama al mismo endpoint
- Entonces responde 403
- Y el body no expone ninguna cifra del evento

#### Scenario: evento inexistente

- Dado que no existe el `idEvento` solicitado
- Cuando el organizador consulta el reporte
- Entonces responde 404

#### Scenario: identidad ausente en el servicio

- Dado que la solicitud llega a `msvc-evento` sin `X-User-Id`
- Cuando el servicio evalúa la pertenencia
- Entonces responde 403
- Y no intenta consultar roles contra `msvc-usuarios`

### Requirement: Métricas por sector [Entrega 2]

El reporte DEBE mostrar entradas vendidas, devueltas, ocupación y recaudación
para cada sector configurado en el evento y para cada sector con ventas
devuelto por `msvc-entrada`.

#### Scenario: ventas y devoluciones por sector

- Dado que el sector 1 tiene 120 entradas `VALIDA`/`USADA` y 5 `DEVUELTA`
- Cuando se genera el reporte
- Entonces `porSector[0].idSector` es 1
- Y `entradasVendidas` es 120 y `entradasDevueltas` es 5
- Y `enReintegro` se informa por separado sin contarse como vendida

#### Scenario: entradas usadas y reservadas

- Dado que una entrada está `USADA` y otra `RESERVADA`
- Cuando se genera el reporte
- Entonces la usada cuenta como vendida y ocupada
- Y la reservada no cuenta en ningún total

#### Scenario: porcentaje de ocupación por sector

- Dado que el sector 1 tiene capacidad 200 y 120 entradas vendidas
- Cuando se genera el reporte
- Entonces `porcentajeOcupacion` es 60.00

#### Scenario: sector sin ventas

- Dado que un sector tiene capacidad 100 y ninguna entrada vendida
- Cuando se genera el reporte
- Entonces `porcentajeOcupacion` es 0.00
- Y el sector sigue presente en `porSector`

#### Scenario: capacidad nula, cero o desconocida

- Dado que un sector tiene `capacidad` nula, 0 o no fue devuelta por
  `SectorClient`
- Pero tiene entradas vendidas
- Cuando se genera el reporte
- Entonces se incluye con `capacidad = 0` y `porcentajeOcupacion = 0.00`
- Y sus conteos y recaudación no se pierden

#### Scenario: porcentaje total del evento

- Dado que el evento tiene capacidad total 300 y 120 entradas vendidas
- Cuando se genera el reporte
- Entonces `totales.porcentajeOcupacion` es 40.00
- Y si la capacidad total es 0, el porcentaje total es 0.00

### Requirement: Recaudación neta [Entrega 2]

El sistema DEBE calcular la recaudación neta descontando únicamente los pagos
cuyo reintegro fue completado.

#### Scenario: reembolso aprobado

- Dado que un pago tiene `RegistroPago.montoTotal = 1000` y estado
  `REEMBOLSADO`
- Cuando se genera el reporte
- Entonces ese monto se incluye en `recaudacionBruta`
- Y en `recaudacionDevuelta`
- Y no se incluye en `recaudacionNeta`

#### Scenario: reintegro pendiente

- Dado que un pago está en `REINTEGRO_PENDIENTE`
- Cuando se genera el reporte
- Entonces su monto sigue contando en `recaudacionBruta` y `recaudacionNeta`
- Y la entrada correspondiente se informa en `enReintegro`

#### Scenario: no doble descuento

- Dado que una entrada está `DEVUELTA` pero su pago aún no está
  `REEMBOLSADO`
- Cuando se genera el reporte
- Entonces la entrada cuenta en `entradasDevueltas`
- Y su monto no se descuenta de `recaudacionNeta` todavía

#### Scenario: sin movimientos

- Dado que el evento no tiene entradas ni pagos
- Cuando se genera el reporte
- Entonces `porSector` está vacío
- Y todos los totales están en 0 o 0.00

### Requirement: Contrato interno de reporte [Entrega 2]

`msvc-evento` DEBE obtener las cifras de ventas y dinero mediante un contrato
interno de `msvc-entrada`, sin exponerlo al público.

#### Scenario: consumo interno

- Dado que `msvc-evento` necesita las cifras del evento 10
- Cuando invoca `GET /internal/entradas/eventos/10/reporte`
- Entonces obtiene `idEvento`, `porSector` y `totales`
- Y `idEvento` coincide con el solicitado
- Y el mismo `totales.entradasVendidas` alimenta el orden por relevancia

#### Scenario: respuesta incompleta o fallo del proveedor

- Dado que `msvc-entrada` responde con un body incompleto, con otro
  `idEvento` o falla la llamada
- Cuando `msvc-evento` procesa el reporte
- Entonces responde 502
- Y no devuelve cifras parciales como si fueran definitivas

#### Scenario: fallo al consultar sectores

- Dado que `SectorClient` falla al consultar nombre o capacidad de un sector
- Cuando se genera el reporte
- Entonces responde 502
- Y no devuelve un reporte incompleto

#### Scenario: contrato único

- Dado que existía `GET /eventos/{idEvento}/ventas` declarado en
  `EntradaClient`
- Cuando se cierra este change
- Entonces ese path queda reemplazado por el contrato interno del reporte
- Y `openspec/config.yaml` nombra una sola ruta de ventas

### Requirement: Reporte como snapshot [Entrega 2]

El reporte DEBE reflejar el estado actual de entradas y pagos al momento de la
consulta.

#### Scenario: sin filtros de fecha

- Dado que un cliente quiere consultar el reporte
- Cuando arma la solicitud
- Entonces no envía parámetros `from`, `to` ni de período
- Y el sistema no expone histórico ni series temporales en este change

### Requirement: Evidencia automática del reporte [Entrega 2]

Cada regla de cálculo y autorización DEBE tener un caso de prueba ejecutable.

#### Scenario: cobertura de la suite

- Dado que se ejecutan las pruebas de `msvc-entrada`, `msvc-evento` y gateway
- Entonces hay tests para dueño, otro organizador, evento inexistente,
  identidad ausente, ocupación, capacidad 0, reservadas, usadas,
  devoluciones, doble descuento, reintegro pendiente, sin movimientos y
  fallos de Feign
- Y `GatewayRoleMatrixTest` falla si se agrega la ruta sin regla de rol
- Y no se modificaron tests existentes para que pasen
