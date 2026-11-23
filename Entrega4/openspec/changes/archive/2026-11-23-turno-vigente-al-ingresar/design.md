# Diseño: turno vigente al ingresar

## Contexto

El diagnóstico aprobado del 409 en `POST /api/compras` estableció que la causa
no es el cupo ni una condición de carrera sobre los locks: es una ventana de
habilitación. `FilaVirtualService.unirse` guarda cada ingreso como `EN_ESPERA`
y solo `abrirTandaSiCorresponde` habilita personas, pero esa función vuelve
en cuanto encuentra una tanda `ABIERTA`. Con ramp-up corto, el primer ingreso
abre la tanda cuando es el único esperando y se habilita a sí mismo; quienes
llegan después quedan `EN_ESPERA` aunque haya cupo. Si compran antes de que un
pago dispare la siguiente tanda, `CompraService.crear` responde 409
`No hay un turno vigente para iniciar la Compra`.

Mediciones del diagnóstico con `maxPersonas=30`, `cupoEntradas=60`, 30 clientes:
con ramp=10 s hubo 30/30 compras OK; con ramp=0 hubo 10/30. Subir
`maxPersonas` a 60 con ramp=0 empeoró a 4/30. No hubo sobreventa en ningún
caso: el sobrante de cupo era real.

## Objetivos

- Que un cliente que se une mientras hay una tanda útil abierta reciba un turno
  habilitado en esa misma tanda, sin requerir un pago ajeno que la reabra.
- Mantener el lote en `maxPersonas`, el orden por secuencia y el cupo atómico
  como único árbitro de la venta.
- Conservar el 409 para quien efectivamente no tiene turno vigente.

## Fuera del alcance

- No se modifica `descontarCupo`, `TandaFilaRepository`, los topes por
  cliente/sector ni `reevaluarCierreTanda`.
- No se modifica `CompraService.crear` ni la validación de turno.
- No se cambia la configuración de tandas (duración de 10 minutos) ni se agrega
  una nueva propiedad, salvo decisión explícita sobre la pregunta abierta.
- No se edita el change archivado `verificacion-e2e-carga`. La spec viva
  `fila-virtual` sí se sincronizó desde el delta de este change antes de
  archivarlo, siguiendo el flujo de OpenSpec.

## Decisión: habilitación directa bajo el lock de evento

`unirse` ya corre dentro de `@Transactional` y toma
`filaEventoRepository.bloquearEventoCompartido(idEvento)`, que serializa a todos
los ingresos del evento incluso con dos réplicas. Dentro de ese mismo bloqueo,
después de asignar la secuencia al nuevo `IngresoFila` y antes de
`saveAndFlush`, se evalúan **todas** estas condiciones:

1. `tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(idEvento)` devuelve una
   tanda `ABIERTA` (el repositorio ya la bloquea).
2. `tanda.getVenceEn().isAfter(ahora)` y `tanda.getCupoRestante() > 0`.
3. `ingresoFilaRepository.buscarHabilitadosDeTanda(idEvento, tanda.getId()).size()
   < fila.getMaxPersonas()`.
4. `ingresoFilaRepository.countByIdEventoAndEstadoAndSecuenciaLessThan(
   idEvento, EN_ESPERA, ingreso.getSecuencia()) == 0`.

Si se cumplen, se hace `ingreso.setEstado(HABILITADO)`,
`ingreso.setIdTanda(tanda.getId())` y `ingreso.setVenceEn(tanda.getVenceEn())`
sobre la entidad que ya se va a persistir. Si falta alguna condición, se
conserva `setEstado(EN_ESPERA)` y el flujo actual: si no hay tanda abierta,
`abrirTandaSiCorresponde` abre una y habilita a los esperando en orden hasta
`maxPersonas`.

Los cuatro métodos de repositorio necesarios ya existen; no hace falta agregar
consultas ni migraciones. La comprobación de `EN_ESPERA` anterior es la que
evita que un recién llegado "salte la fila" cuando la tanda abierta todavía no
llenó el lote.

### Por qué no tocar la reserva

Habilitar más personas no vende más entradas por sí solo: cada compra sigue
pasando por `descontarCupo`, que es atómico y compara `cupo_restante >= cantidad`.
Si la demanda supera el cupo, el excediente recibe el 409 de cupo existente y
vuelve a `EN_ESPERA` conservando su secuencia. Anti doble venta queda intacto.

## Concurrencia y orden

- La secuencia se asigna desde `fila.getSiguienteSecuencia()` dentro del lock de
  evento, por lo que es única y creciente por evento incluso con dos réplicas.
- El conteo de habilitados y de esperadores anteriores se hace dentro del mismo
  lock, así que la decisión es linealizable: no dos hilos pueden creerse el
  último lugar del lote.
- Un reintento de `unirse` para el mismo usuario sigue devolviendo su membresía
  vigente antes de crear una nueva (comportamiento actual).

## Decisiones rechazadas

- **Aumentar `maxPersonas`**: no cambia el momento de habilitación y empeoró la
  corrida con ramp=0 (26/30 rechazos con 60).
- **Habilitar en `CompraService.crear`**: mezclaría la validación de turno con
  la mutación de la fila y duplicaría la lógica de lotes. Además obligaría a
  modificar el servicio que este change deja intacto.
- **Habilitar aunque haya `EN_ESPERA` anterior**: rompería el orden de llegada,
  que es una garantía de la spec de fila virtual.

## Decisión del grupo: mínimo de tiempo restante de la tanda

El grupo decidió **no incorporar** un mínimo de tiempo restante. La tanda se
abre con 10 minutos de vigencia y el turno se usa de inmediato en el flujo E2E;
agregar un umbral introduciría una nueva propiedad y un motivo adicional para
quedar `EN_ESPERA` sin evidencia de que evite el 409 original (que era de turno
inexistente, no de turno corto). La condición de habilitación se limita por tanto
a `venceEn.isAfter(ahora)` y `cupoRestante > 0`, sin nueva configuración.

## Limitación conocida: vigencia mínima del turno

No hay umbral mínimo de tiempo restante al habilitar. La condición es solo
`venceEn.isAfter(ahora)`, así que quien entra en los últimos segundos de una
tanda recibe un turno casi inútil. El impacto está acotado por la vigencia fija
de 10 minutos por tanda y por la apertura de la siguiente cuando se cierra la
anterior, además de que el comprobante de cupo se revalida al comprar. El grupo
ya decidió no agregar configuración nueva para esto; **no se propone una
corrección dentro de este change**. Si se quiere revisar, requiere una decisión
explícita y su propio change con tests.

## Hallazgos de verificación posterior al archivado (2026-11-23)

Se registran aquí dos huecos detectados al revisar el estado del cliente.
**Ambos quedaron resueltos** en el change
[`corregir-tope-lote-fila`](../../corregir-tope-lote-fila/) (tests verdes y suite
de `msvc-entrada` en 212/212). La propuesta 2B de exponer `estadoFila`/`motivo`
es una **mejora futura pendiente de aprobación**, no un defecto abierto ni un
cambio incluido en este arreglo, porque modificaría el contrato de la API.

### 1. RESUELTO — El lote de una tanda podía superar `maxPersonas` cuando otros ya compraron

`IngresoFilaRepository.buscarHabilitadosDeTanda` (`IngresoFilaRepository.java:54-63`)
filtra `i.estado = 'HABILITADO'`. `CompraService.pagar` cambia la membresía a
`COMPRO` (`CompraService.java:419-422`), así que el conteo de
`FilaVirtualService.java:199` baja a medida que la gente compra. Mientras la
tanda siga `ABIERTA` con cupo, un recién llegado ve `habilitados < maxPersonas`
y se habilita en esa misma tanda: el total de personas asignadas puede pasar del
lote.

Evidencia original: el test `CompraConcurrenciaIntegrationTest.loteNoSuperaMaxPersonasCuandoDiezYaCompraronEnLaMismaTanda`
(30 altas concurrentes, 10 compras pagadas, 5 altas concurrentes más; tanda
abierta con 100 de cupo restante) fallaba con
`expected: <30> but was: <35>`. Las corridas 30/30 y 45/30 no lo detectaban porque
todos los clientes se unen antes de que alguien compre.

Resolución en `corregir-tope-lote-fila`: se agregó
`IngresoFilaRepository.contarAsignadosDeTanda(idEvento, idTanda)` y se usa ese
`count` en la condición de habilitación. `maxPersonas` pasa a ser el tamaño total
del lote por tanda, no el de `HABILITADOS` simultáneos. El test ya pasa y se
agregó `loteCuentaCompradoresYTurnosVencidosSinSuperarMaxPersonas` (10 `COMPRO` +
5 `VENCIDO`). No se tocó el conteo `HABILITADO` usado para cerrar tandas
(`CompraService.java:774`, `FilaVirtualService.java:299,330`).

### 2. RESUELTO (500) / pendiente (UX) — `GET /mi-estado` no informa por qué la fila está agotada

Con el evento 20 de la corrida B, `fila_evento.estado = AGOTADA`,
`tanda_fila.estado = CERRADA` y `cupo_restante = 0`; 15 clientes siguen
`EN_ESPERA`. El endpoint devuelve solo el estado de la membresía, sin el estado
de la fila ni un motivo:

```json
{"idEvento":20,"idIngreso":485,"secuencia":34,"estado":"EN_ESPERA","posicion":4,"venceEn":null,"idTanda":null}
```

El cliente puede seguir haciendo polling indefinidamente sin saber que no hay
cupo: ese hueco de UX **sigue pendiente** (propuesta 2B).

El HTTP 500 de quien ya compró (`estado = COMPRO`) sí quedó **resuelto** en
`corregir-tope-lote-fila`: `FilaVirtualService.convertirIngreso` promovía el
ternary de `posicion` a `long` y hacía unboxing de `null`
(`Cannot invoke "java.lang.Long.longValue()"`). Ahora usa ramas explícitas y
devuelve `posicion: null` con HTTP 200 para `COMPRO`, `VENCIDO`, `CANCELADO` y
`SALIO`, sin cambiar el contrato. Los tests de regresión cubren esos cuatro
estados y conservan `posicion = 4`/`0` para `EN_ESPERA`/`HABILITADO`.

Mejora futura 2B, **pendiente de aprobación y no implementada**: agregar al DTO
un campo opcional `motivo` (por ejemplo `FILA_AGOTADA`) o exponer `estadoFila`,
calculado con la misma transacción de consulta. No es un defecto abierto ni un
cambio incluido en `corregir-tope-lote-fila`; cambiaría el contrato de
`IngresoFilaResponse`, por lo que requiere aprobación, change OpenSpec y
actualización de la spec viva.

### 3. Nota — fallo intermitente por orden de hilos en el test

El fallo intermitente de
`CompraConcurrenciaIntegrationTest.habilitacionRespetaOrdenDeSecuenciaYNoAdelantaAQuienEsperaba`
fue de un test que suponía el orden de los hilos: fijaba al usuario `52` como el
tercero, aunque los tres `unirse` concurrentes asignan secuencias en orden no
determinista. La corrección captura el `idUsuario` real del ingreso `EN_ESPERA`
antes de cerrar la tanda.

La ausencia de carrera entre habilitación y vencimiento se razonó por inspección
de los locks: `unirse` toma advisory compartido (`bloquearEventoCompartido`) y
`FOR UPDATE` de `fila_evento`; `cerrarTandaVencida` toma advisory exclusivo
(`bloquearEvento`) y el mismo bloqueo de fila. No se probó con un test de
concurrencia entre ambas operaciones; no se agrega ese test ahora.

## Riesgos

- *Cambio de comportamiento especificado*: hoy la spec dice que al ingresar con
  tanda en curso siempre se queda `EN_ESPERA`. Se modifica solo ese escenario
  en el delta de este change; por eso se necesita un change nuevo y no una
  edición del archivado.
- *Turno habilitado que nadie usa*: ya existe el vencimiento por scheduler; el
  fix no altera esos plazos.
- *Sampler que enmascare el defecto*: el plan JMeter original espera 10 s de
  ramp-up y encadena pago aunque falle la compra. Por eso se corrige el sampler
  y se ejecutan las dos corridas (ramp=0 y ramp=10 s) con el mismo criterio de
  integridad.

## Verificación

- Tests de regresión con Testcontainers y `CountDownLatch`/`ExecutorService`:
  deben fallar en rojo antes del fix y pasar en verde después.
- `mvn -pl msvc-entrada test` sin regresiones.
- Dos corridas JMeter de 30 clientes, 2 réplicas, `maxPersonas=30`: ramp=0 y
  ramp=10 s, con p95, conteo de errores y verificación de integridad (compras
  pagadas = compras OK, entradas = 2 por compra, claves de idempotencia
  distintas, 0 sobreventa).
- Corrida de 45 clientes con `maxPersonas=30` y timeout de polling de 60 s:
  tope de 30 habilitados en la primera tanda, FIFO verificado en base y una
  corrida de control con tope global de 60 entradas (`resultados/carga-45.md`).
- Análisis de la latencia ramp=0 vs ramp=10 con distribución por orden de
  llegada, repeticiones y recuento de muestras del p95 (`resultados/latencia.md`).
- `openspec validate turno-vigente-al-ingresar --strict` en forma aislada y
  `openspec validate fila-virtual --type spec --strict` para la spec sincronizada.
- `openspec validate --specs --json` solo en modo lectura, para confirmar que la
  deuda preexistente de 6 specs sin `## Purpose` no aumenta; la única spec viva
  editada es `fila-virtual`, que quedó válida.

## Cobertura de los escenarios

| Escenario | Test o corrida que lo cubre |
|---|---|
| primer ingreso e ingreso repetido | `FilaVirtualServiceTest.primerIngresoAbreTandaCuandoLaFilaActivaNoTieneUnaEnCurso`, `CompraConcurrenciaIntegrationTest.postgresPermiteUnaSolaMembresiaVigentePorEventoYUsuario` y `unaSolaCompraReservadaComproTerminalYReingresoConNuevaSecuencia` |
| ingreso sin tanda en curso | `FilaVirtualServiceTest.primerIngresoAbreTandaCuandoLaFilaActivaNoTieneUnaEnCurso` |
| turno útil con/sin umbral de tiempo | `nConcurrentesSobreCapacidadNCompranTodosSinRechazo`, `doceIngresosConcurrentesRespetanTopeMaxPersonas`, `habilitacionRespetaOrdenDeSecuenciaYNoAdelantaAQuienEsperaba` + corridas `ramp0`/`ramp10` |
| tanda abierta sin turno útil | `FilaVirtualServiceTest.nuevoIngresoEsperaLaTandaSiguienteCuandoYaHayUnaAbierta` (tanda sin `venceEn`/`cupoRestante`) |
| tope de `maxPersonas` | `doceIngresosConcurrentesRespetanTopeMaxPersonas` |
| orden de llegada | `habilitacionRespetaOrdenDeSecuenciaYNoAdelantaAQuienEsperaba` |
| N concurrentes sobre capacidad N | `nConcurrentesSobreCapacidadNCompranTodosSinRechazo` + corrida `ramp0` (30/30) |
| demanda mayor al cupo | `demandaMayorAlCupoVendeExactamenteCupoEntradas` |
| secuencia conservada tras falta de cupo | `rechazoPorCupoAgotadoCierraTandaYDevuelveHabilitadosAEsperaConSuSecuencia` y `cierrePorCupoDevuelveAlHabilitadoSinRechazoConservandoSecuencia` |
| turno vencido requiere nuevo ingreso | `unaSolaCompraReservadaComproTerminalYReingresoConNuevaSecuencia` y `cierrePorTiempoExpulsaSoloAQuienNoComproNiRegistroRechazo` |
| integridad anti doble venta en carga | `resultados/carga.md`: compras pagadas, idempotencia, entradas y 0 sobreventa en ambas corridas |
| tope de `maxPersonas` tras compras dentro de la tanda | **Resuelto** en `corregir-tope-lote-fila`: `loteNoSuperaMaxPersonasCuandoDiezYaCompraronEnLaMismaTanda` y `loteCuentaCompradoresYTurnosVencidosSinSuperarMaxPersonas` (30 asignados, no 35) |
| tope y FIFO bajo carga con demanda > lote | `resultados/carga-45.md`: 30/45 en la primera tanda, 0 inversiones en base, 15 habilitados al cerrarla (corrida A) o 15 en espera por cupo global (corrida B) |
| latencia con ramp=0 | `resultados/latencia.md`: gradiente por orden de llegada en 3 repeticiones, p95 sostenido por 2 de 30 muestras |
