# Propuesta: turno vigente al ingresar

## Por qué

El diagnóstico del change archivado `verificacion-e2e-carga` mostró que, con ramp-up
corto, el primer cliente que se une abre la tanda y es el único `HABILITADO`; los
demás quedan `EN_ESPERA` porque `abrirTandaSiCorresponde` no habilita a nadie
cuando ya hay una tanda `ABIERTA`. Si compran antes de que un pago dispare la
siguiente tanda, reciben 409 `No hay un turno vigente para iniciar la Compra`,
aunque haya cupo real. Subir `maxPersonas` a 60 no corrige el defecto: con
ramp=0 dio peor resultado (26/30 rechazos) que 30 (20/30). La regla operativa
documentada era un artefacto del ramp-up y del sampler, no una regla de
dimensionamiento.

## Qué cambia

- **`FilaVirtualService.unirse` habilita al recién llegado en la tanda abierta**
  cuando se cumplen simultáneamente: tanda `ABIERTA`, vigente
  (`venceEn > ahora`), `cupoRestante > 0`, habilitados de esa tanda < `maxPersonas`,
  y ninguna persona `EN_ESPERA` con secuencia menor. En caso contrario se
  conserva el comportamiento actual (`EN_ESPERA`).
- **El lote deja de reducirse a una persona**: bajo demanda simultánea se
  habilita hasta `maxPersonas` en orden de secuencia, sin adelantar a quienes
  ya esperaban.
- **Tests de regresión** con Testcontainers + `CountDownLatch`/`ExecutorService`:
  cobertura de N concurrentes sobre capacidad N, tope de `maxPersonas`, orden
  de llegada y venta exacta hasta agotar `cupoEntradas`.
- **Sampler JMeter corregido**: espera a `HABILITADO` con timeout antes de
  comprar y no encadena el pago si la compra falla.
- **Documentación corregida**: se reemplaza la regla operativa del informe
  archivado por una nota que explica el artefacto del ramp-up/sampler. No se
  edita el change archivado: la nota y el cambio de comportamiento viven en la
  spec delta de este change. La sincronización a `openspec/specs/` queda como
  seguimiento posterior para no tocar la deuda preexistente de validación.

### Fuera del alcance

- No se tocan `descontarCupo`, los topes por cliente/sector/compra ni la lógica
  de cierre y reapertura de tandas.
- No se modifica `CompraService.crear`, la validación de turno ni los códigos
  de error: el 409 sigue existiendo para quien realmente no tiene turno.
- No se agrega un umbral mínimo de tiempo restante de tanda (ver `design.md`).
- No se cambian secretos, compose ni el estado archivado de
  `verificacion-e2e-carga`. No se hace commit ni push.

## Capabilities

### Agregadas

- Ninguna.

### Modificadas

- `fila-virtual`: se modifica el requisito "Ingreso idempotente y orden estable
  de la fila [Entrega 2]" (hoy en el delta del change activo `fila-virtual`,
  pendiente de sincronizar a `openspec/specs/fila-virtual/spec.md`). El escenario
  "ingreso mientras hay una tanda en curso" pasa de "siempre EN_ESPERA" a
  "HABILITADO si hay turno útil, cupo, lugar en el lote y nadie delante esperando".

## Impact

- Código: `msvc-entrada/src/main/java/com/ticketya/entrada/service/FilaVirtualService.java`
  y su repositorio de ingresos para el conteo de habilitados dentro del lock.
- Tests: `msvc-entrada/src/test/java/com/ticketya/entrada/integration/CompraConcurrenciaIntegrationTest.java`
  (o archivo nuevo junto a él).
- Artefactos de carga: nuevo plan JMeter derivado de `prueba-carga.jmx`, con
  datos crudos y resultados en este change; sin informe HTML versionado.
- Documentación: nota de corrección en este change y actualización de la spec
  delta de `fila-virtual`; el informe archivado queda intacto.
- No hay cambios de API pública, dependencias ni ADR nuevos: se reutiliza el
  lock de evento ya existente y la cola persistida definida en el contexto.
