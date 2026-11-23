# Tasks

## 0. Aprobación del grupo (bloqueante)

- [x] 0.1 Revisar `proposal.md`, `specs/fila-virtual/spec.md`, `design.md` y este `tasks.md`; verificar con `openspec validate turno-vigente-al-ingresar --strict` que el change es válido de forma aislada y corregir lo que indique la CLI.
- [x] 0.2 Resolver la pregunta abierta de `design.md`: el grupo decidió no incorporar el mínimo de tiempo restante de la tanda; queda registrado en `design.md`.
- [x] 0.3 No escribir código, configuración ni artefactos de carga (Dockerfiles, YAML, JMX, specs vivas) hasta que 0.1 y 0.2 estén cerrados; no hacer commit ni push. Verificado con `git status --short`: el único cambio nuevo era este directorio.

## 1. Tests de regresión (rojo)

- [x] 1.1 Agregar en `msvc-entrada/src/test/java/com/ticketya/entrada/integration/CompraConcurrenciaIntegrationTest.java` (o archivo nuevo junto a él) cuatro tests con Testcontainers + `CountDownLatch`/`ExecutorService`: (a) N concurrentes con `maxPersonas=N` y `cupoEntradas=2N` → N compras pagadas, 2N entradas, 0 rechazos, 0 sobreventa; (b) 12 ingresos concurrentes con `maxPersonas=8` → exactamente 8 `HABILITADO` y 4 `EN_ESPERA`; (c) con alguien `EN_ESPERA` de secuencia menor, el recién llegado no lo adelanta; (d) demanda mayor que `cupoEntradas` → se venden exactamente C entradas y 0 de más.
- [x] 1.2 Ejecutar `mvn -pl msvc-entrada test -Dtest=CompraConcurrenciaIntegrationTest` **sin el fix** y registrar en `resultados/antes-fix.md` que los cuatro tests fallan por el comportamiento actual (1 habilitado, 409 sin turno, adelantamiento o sobreventa, según corresponda). Verificar que el resto de la clase sigue en verde.

## 2. Fix en FilaVirtualService (verde)

- [x] 2.1 Implementar en `FilaVirtualService.unirse`, dentro de `bloquearEventoCompartido` y antes de `saveAndFlush`, la habilitación directa solo si hay tanda `ABIERTA`, `venceEn` posterior a `ahora`, `cupoRestante > 0`, habilitados < `maxPersonas` y 0 personas `EN_ESPERA` con secuencia menor; reutilizar `buscarAbiertaPorEventoConBloqueo`, `buscarHabilitadosDeTanda` y `countByIdEventoAndEstadoAndSecuenciaLessThan`. Verificar que los cuatro tests del grupo 1 pasan.
- [x] 2.2 Verificar con `git diff -- msvc-entrada/src/main/java/com/ticketya/entrada/service/CompraService.java msvc-entrada/src/main/java/com/ticketya/entrada/repository/TandaFilaRepository.java` que no hay cambios en `descontarCupo`, topes ni cierre de tanda; verificar que no se agregó configuración nueva salvo decisión del grupo 0.2.
- [x] 2.3 Ejecutar `mvn -pl msvc-entrada test` y verificar que la suite completa pasa sin regresiones. Registrar el resultado en `resultados/despues-fix.md`.

## 3. Sampler JMeter

- [x] 3.1 Copiar `prueba-carga.jmx` del change archivado a `jmeter/prueba-carga.jmx` en este change y agregar, después de `unirse`, un polling con timeout acotado de `GET` al estado de la fila hasta que el estado sea `HABILITADO`. Verificar con una corrida corta que ninguna compra se envía en `EN_ESPERA`.
- [x] 3.2 Agregar una condición que impida continuar al pago cuando la compra no devuelve `idCompra`; verificar en el JTL que no aparece `SIN_ID` ni un 400 de pago derivado de una compra fallida.
- [x] 3.3 Documentar en `jmeter/README.md` las properties del plan (`runId`, tokens, hilos, ramp, `maxPersonas`, `cupoEntradas`) y el comando de generación local del informe HTML. Verificar que no se versionan `.env`, `secrets/`, tokens ni `jmeter.log`.

## 4. Prueba de carga (2 réplicas)

- [x] 4.1 Verificar `docker compose ps` con 11 contenedores en marcha y 2 réplicas de `msvc-entrada`; crear clientes, evento y fila nuevos para cada corrida sin modificar compose ni configuración versionada. Verificar que los tokens quedan solo en `C:\Users\lauty\AppData\Local\Temp\opencode\diag\`.
- [x] 4.2 Ejecutar la corrida `ramp0`: 30 clientes, 2 entradas por compra, `maxPersonas=30`, `cupoEntradas=60`, 30 hilos, ramp=0. Verificar compras OK, errores, p95 y conteo por mensaje de error; guardar el JTL en este change sin `jmeter.log`.
- [x] 4.3 Ejecutar la corrida `ramp10`: mismos parámetros salvo ramp=10 s. Verificar compras OK, errores, p95 y que el resultado sea comparable con el diagnóstico (30/30 sin 409 de turno).
- [x] 4.4 Escribir `resultados/carga.md` con ambas corridas y la verificación de integridad: compras pagadas, entradas emitidas, claves de idempotencia distintas, 0 sobreventa y 0 pagos `SIN_ID`. Si con ramp=0 quedan clientes `EN_ESPERA` por exceder `maxPersonas` o el cupo, indicar cuántos, la causa y confirmar con una consulta posterior que terminan comprando al ser habilitados.
- [x] 4.5 Generar el informe HTML solo en local con `jmeter -g resultados/jtl/ramp0.jtl -o <temp>\informe-ramp0` (y equivalente para ramp10). Verificar con `git status --short` que no se versiona HTML ni log.

## 5. Documentación y spec

- [x] 5.1 Crear `nota-maxpersonas.md` en este change reemplazando la regla operativa: explicar que dimensionar `maxPersonas` con margen era un artefacto del ramp-up de 10 s y del sampler que no esperaba `HABILITADO`, citar las cifras ramp=0 (30 → 10/30; 60 → 4/30) y aclarar que ahora el lote se llena en orden de llegada. Verificar con `git diff --name-only` que el change archivado no aparece.
- [x] 5.2 Ajustar `specs/fila-virtual/spec.md` de este change para que los escenarios reflejen exactamente el comportamiento implementado en 2.1 y los casos verificados en 4.4. Verificar que cada escenario tiene un test o una corrida que lo cubre.
- [x] 5.3 Ejecutar `openspec validate turno-vigente-al-ingresar --strict` y verificar que pasa; ejecutar `openspec validate --specs --json` solo para confirmar que la deuda preexistente sigue siendo 6 specs sin `## Purpose` y que este change no la aumenta. Verificar que no se editó ninguna spec viva.

## Workflow follow-up

- Revisar y archivar el change cuando el grupo acepte los resultados; verificar el archivado con `openspec validate --archived`.
- Sincronización completada en este change antes de archivar: se creó `openspec/specs/fila-virtual/spec.md` desde el delta (con `## Purpose`, sin aumentar la deuda preexistente) y se validó de forma aislada con `openspec validate fila-virtual --type spec --strict`.
- Corrida 45/30, análisis de latencia y limitación de vigencia documentados en `resultados/carga-45.md`, `resultados/latencia.md` y `design.md`.
- No hacer commit ni push sin autorización explícita.
