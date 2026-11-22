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
- Y el body contiene `idEvento`, `estadoEvento`, `porSector` y `totales`

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
- Y se distingue del caso de evento ajeno, que responde 403 según la política
  vigente de `identidad-servicios`

#### Scenario: identidad ausente en el servicio

- Dado que la solicitud llega a `msvc-evento` sin `X-User-Id`
- Cuando el servicio evalúa la pertenencia
- Entonces responde 403
- Y no intenta consultar roles contra `msvc-usuarios`

#### Scenario: evento cancelado

- Dado que el evento está `CANCELADO` y pertenece al organizador
- Cuando consulta el reporte
- Entonces responde 200 con `estadoEvento = "CANCELADO"`
- Y el body muestra el snapshot final con devoluciones y reembolsos ya
  descontados, sin cifras vigentes inventadas

### Requirement: Métricas por sector [Entrega 2]

El reporte DEBE mostrar entradas emitidas, activas, devueltas y en reintegro,
además de la ocupación y la recaudación, para cada sector configurado en el
evento y para cada sector con ventas devuelto por `msvc-entrada`.

`entradasEmitidas` es el histórico `VALIDA + USADA + EN_REINTEGRO + DEVUELTA`.
`entradasActivas` son las vigentes `VALIDA + USADA`. La ocupación se calcula
sobre las activas.

#### Scenario: emitidas, activas, devueltas y en reintegro por sector

- Dado que el sector 1 tiene 120 entradas `VALIDA`/`USADA`, 5 `DEVUELTA` y 2
  `EN_REINTEGRO`
- Cuando se genera el reporte
- Entonces `porSector[0].idSector` es 1
- Y `entradasEmitidas` es 127, `entradasActivas` es 120, `entradasDevueltas`
  es 5 y `enReintegro` es 2
- Y `enReintegro` no cuenta como activa ni ocupa

#### Scenario: entradas usadas, en reintegro y reservadas

- Dado que una entrada está `USADA`, otra `EN_REINTEGRO` y otra `RESERVADA`
- Cuando se genera el reporte
- Entonces la usada cuenta como emitida, activa y ocupada
- Y la de `EN_REINTEGRO` cuenta como emitida e informada aparte, pero no como
  activa ni ocupada
- Y la reservada no cuenta en ningún campo

#### Scenario: porcentaje de ocupación por sector

- Dado que el sector 1 tiene capacidad 200 y 120 entradas activas
- Cuando se genera el reporte
- Entonces `porcentajeOcupacion` es 60.00

#### Scenario: sector sin ventas

- Dado que un sector tiene capacidad 100 y ninguna entrada activa
- Cuando se genera el reporte
- Entonces `porcentajeOcupacion` es 0.00
- Y el sector sigue presente en `porSector`

#### Scenario: capacidad nula, cero o desconocida

- Dado que un sector tiene `capacidad` nula, 0 o no fue devuelta por
  `SectorClient`
- Pero tiene entradas emitidas
- Cuando se genera el reporte
- Entonces se incluye con `capacidad = 0` y `porcentajeOcupacion = 0.00`
- Y sus conteos y recaudación no se pierden

#### Scenario: porcentaje total del evento

- Dado que el evento tiene capacidad total 300 y 120 entradas activas
- Cuando se genera el reporte
- Entonces `totales.porcentajeOcupacion` es 40.00
- Y si la capacidad total es 0, el porcentaje total es 0.00

#### Scenario: consistencia entre sectores y totales

- Dado que un evento tiene varios sectores con entradas y recaudación
- Cuando se genera el reporte
- Entonces la suma de cada campo de `porSector` es igual al campo
  correspondiente de `totales`
- Y la regla se cumple para emitidas, activas, devueltas, en reintegro y para
  bruta, devuelta y neta

#### Scenario: aislamiento entre eventos

- Dado que el evento 10 y el evento 20 pertenecen al mismo organizador
- Y el evento 10 tiene entradas emitidas y el evento 20 tiene otras distintas
- Cuando se consulta el reporte del evento 10
- Entonces el body no incluye ninguna entrada, sector ni importe del evento 20
- Y el reporte del evento 20 tampoco incluye datos del evento 10

### Requirement: Recaudación neta [Entrega 2]

El sistema DEBE calcular la recaudación neta descontando únicamente los pagos
cuyo reintegro fue completado. La fuente es `RegistroPago.montoTotal`, que es
un importe por entrada.

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

#### Scenario: monto por entrada, no por compra

- Dado que un usuario compra dos entradas del mismo evento y se aprueban dos
  pagos de 1000 cada uno
- Cuando se genera el reporte
- Entonces hay dos `RegistroPago` y la recaudación bruta es 2000
- Y no se duplica ni se omite ningún importe por tratarse del mismo comprador

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
- Y `totales.entradasActivas` alimenta el orden por relevancia, no
  `entradasEmitidas`

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

- Dado que un organizador quiere consultar el reporte
- Cuando arma la solicitud
- Entonces no envía parámetros `from`, `to` ni de período
- Y el sistema no expone histórico ni series temporales en este change

### Requirement: Evidencia automática del reporte [Entrega 2]

Cada regla de cálculo y autorización DEBE tener un caso de prueba ejecutable.

#### Scenario: cobertura de la suite

- Dado que se ejecutan las pruebas de `msvc-entrada`, `msvc-evento` y gateway
- Entonces hay tests para dueño, otro organizador (403), evento inexistente
  (404), evento cancelado (200), identidad ausente, ocupación, capacidad 0,
  reservadas, usadas, en reintegro, devoluciones, doble descuento, pago
  `REEMBOLSADO`, dos entradas aprobadas, sin movimientos, suma de sectores,
  aislamiento entre eventos y fallos de Feign
- Y `GatewayRoleMatrixTest` falla si se agrega una ruta sin regla de rol
- Y los casos de identidad del gateway viven en `GatewayErrorHandlingTest`, no
  en `GatewayRoleMatrixTest`
- Y la prueba de integración se ejecuta contra `docker-compose`, sin
  Testcontainers nuevo
- Y no se modificaron tests existentes para que pasen
