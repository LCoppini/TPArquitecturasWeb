# Tasks

## 1. Verificación previa

- [x] 1.1 Confirmar con código y datos que los estados terminales no borran `idTanda`: sin `setIdTanda(null)` en producción; 349 `COMPRO` y 132 `VENCIDO` en base conservan `id_tanda`; solo 15 `EN_ESPERA` nunca habilitados lo tienen NULL.
- [x] 1.2 Confirmar que los conteos de cierre de tanda siguen usando `buscarHabilitadosDeTanda` y no se tocan.

## 2. Fixes

- [x] 2.1 Agregar `IngresoFilaRepository.contarAsignadosDeTanda(idEvento, idTanda)` con `count` sobre todas las membresías de la tanda.
- [x] 2.2 Usar ese `count` en `habilitarEnTandaAbiertaSiCorresponde`, reemplazando `buscarHabilitadosDeTanda(...).size()`.
- [x] 2.3 Corregir `convertirIngreso` para devolver `posicion = null` en estados distintos de `EN_ESPERA`/`HABILITADO`, sin cambiar el contrato.

## 3. Tests de regresión

- [x] 3.1 El test rojo `loteNoSuperaMaxPersonasCuandoDiezYaCompraronEnLaMismaTanda` pasa con el fix (30 asignados, no 35).
- [x] 3.2 Nuevo test `loteCuentaCompradoresYTurnosVencidosSinSuperarMaxPersonas`: 30 habilitados, 10 compran, 5 quedan `VENCIDO` conservando `idTanda`; los 5 nuevos quedan `EN_ESPERA` y el total asignado sigue siendo 30.
- [x] 3.3 Tests de `mi-estado`: estados terminales (`COMPRO`, `VENCIDO`, `CANCELADO`, `SALIO`) devuelven 200 con `posicion = null`; `EN_ESPERA` y `HABILITADO` conservan 4 y 0.
- [x] 3.4 Suite completa `mvn -pl msvc-entrada test`: 212 tests, 0 fallos, 0 errores, 6 skipped.

## 4. Especificación y documentación

- [x] 4.1 Agregar al delta `specs/fila-virtual/spec.md` el requisito y el escenario del lote con estados terminales.
- [x] 4.2 Sincronizar el mismo requisito en `openspec/specs/fila-virtual/spec.md`.
- [x] 4.3 Validar de forma aislada el change y la spec; no tocar las 6 specs con deuda de `## Purpose` ni el fallo preexistente de `reportes-organizador`.
- [x] 4.4 Actualizar la sección de hallazgos del change archivado `turno-vigente-al-ingresar` para marcar ambos defectos como resueltos y enlazar este change.

## 5. Verificación de carga

- [x] 5.1 Repetir la corrida de 45 clientes, `maxPersonas=30`, `cupoEntradas=60`, `ramp=0`, sector de 1000 lugares, con el artefacto corregido desplegado en 2 réplicas; verificar 45/45 compras, tanda 1 = 30, tanda 2 = 15 y 0 inversiones FIFO.
- [x] 5.2 Grep de secretos en los 34 archivos `.jtl`/`.jmx`/`.md` a versionar: 72 coincidencias documentales o placeholders (`Bearer ${...}`, `<token>`, `JWT_PRIVATE_KEY`) y 0 literales de credencial; 0 JTL con `Authorization`/`Bearer`/`token`. `.env` y `secrets/` siguen ignorados por Git; no hay `jmeter.log`, informes HTML ni el flag de error verbose en el repo.

## Workflow follow-up

- Dejar el change activo a la espera de revisión; archivar cuando el grupo lo apruebe (`openspec archive corregir-tope-lote-fila`).
- No hacer commit ni push sin autorización explícita.
