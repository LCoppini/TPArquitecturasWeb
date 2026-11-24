# Propuesta: corregir tope del lote por tanda

## Por qué

La verificación posterior al archivado de `turno-vigente-al-ingresar` encontró
dos defectos:

1. `habilitarEnTandaAbiertaSiCorresponde` medía el lote con
   `buscarHabilitadosDeTanda(...).size()`, que solo cuenta `HABILITADO`. Como al
   pagar la membresía pasa a `COMPRO`, el conteo bajaba y un recién llegado podía
   habilitarse en una tanda que ya tenía 30 asignados. El test
   `loteNoSuperaMaxPersonasCuandoDiezYaCompraronEnLaMismaTanda` fallaba con
   `expected: <30> but was: <35>`.
2. `GET /mi-estado` respondía HTTP 500 para ingresos en estado `COMPRO` u otro
   terminal: el ternary de `posicion` se promovía a `long` y hacía unboxing de
   `null` (`FilaVirtualService.java:500-504`).

## Qué cambia

- `IngresoFilaRepository.contarAsignadosDeTanda(idEvento, idTanda)` cuenta todas
  las membresías asignadas a la tanda, sin filtrar por estado; la condición de
  habilitación usa ese `count` en lugar del `size` de habilitados.
- `convertirIngreso` calcula `posicion` con ramas explícitas: `EN_ESPERA` →
  posición en la espera, `HABILITADO` → 0, cualquier otro estado → `null`. El
  contrato JSON y los códigos HTTP no cambian.
- Tests de regresión: el test rojo existente, un test con 10 `COMPRO` + 5
  `VENCIDO`, y tests de `mi-estado` para estados terminales.

## Fuera del alcance

- No se modifican los conteos de `HABILITADO` usados para cerrar tandas
  (`CompraService.java:774`, `FilaVirtualService.java:299,330`).
- No se cambia `descontarCupo`, la reserva atómica ni las reglas de FIFO.
- No se agrega `estadoFila`/`motivo` a `IngresoFilaResponse` (propuesta 2B,
  pendiente de aprobación).
- No se hacen cambios de configuración, commit ni push.

## Capabilities

### Agregadas

- Ninguna.

### Modificadas

- `fila-virtual`: nuevo requisito "Tope de lote por tanda con estados terminales",
  con el escenario "el lote no supera `maxPersonas` aunque algunos ya hayan
  comprado" y el de turnos vencidos que siguen ocupando lugar.

## Impact

- Código: `FilaVirtualService.java`, `IngresoFilaRepository.java`.
- Tests: `CompraConcurrenciaIntegrationTest.java`, `FilaVirtualServiceTest.java`.
- Specs: delta de este change y spec viva `openspec/specs/fila-virtual/spec.md`.
- Documentación: sección de hallazgos del change archivado
  `turno-vigente-al-ingresar` pasa a marcar ambos defectos como resueltos.
