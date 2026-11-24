# Nota: `maxPersonas` no es una regla de dimensionamiento

Esta nota reemplaza la "regla operativa" publicada en el documento suelto del
change archivado `verificacion-e2e-carga`:

> "Regla operativa: dimensionar `maxPersonas` por encima del número de
> clientes concurrentes de la prueba."

**Esa conclusión era incorrecta.** No se edita el change archivado; se deja
registrado aquí el criterio vigente para este change y sus seguidores.

## Qué pasó realmente

La correlación con `maxPersonas` era un artefacto de dos factores del plan de
carga original, no una propiedad del sistema:

1. **Ramp-up de 10 s.** Repartía las llegadas en el tiempo, así que el primer
   ingreso abría la tanda y los siguientes iban siendo habilitados por los
   eventos de pago. El plan parecía sano solo porque nadie compraba en el mismo
   instante.
2. **Sampler que no esperaba `HABILITADO`.** En cuanto se comprimía el
   ramp-up, el sampler enviaba `POST /api/compras` apenas terminaba
   `POST /ingresos`, sin consultar `GET /mi-estado`. Quien no era el primer
   llegado seguía `EN_ESPERA` y recibía el 409
   `No hay un turno vigente para iniciar la Compra`, aunque hubiera cupo y
   capacidad.

Las cifras del diagnóstico con `ramp=0`, 30 clientes y `cupoEntradas=60` fueron:

| `maxPersonas` | Compras OK | Interpretación errónea |
|---:|---:|---|
| 30 | 10/30 | "hay que subir el tamaño del lote" |
| 60 | 4/30 | empeoró: agrandar el lote no cambia *cuándo* se habilita |

Que `maxPersonas=60` rindiera **peor** (4/30) descarta la regla: el parámetro
solo decide cuántas personas caben por lote, no habilita a nadie antes. El
defecto era de ventana de habilitación en `FilaVirtualService.unirse`, no de
dimensionamiento.

## Criterio vigente

- `maxPersonas` es el **tamaño del lote** de habilitación por tanda
  (`PageRequest.of(0, maxPersonas)`), no la capacidad del sector ni un margen
  de seguridad frente a la concurrencia. `cupoEntradas` es una dimensión
  separada y sigue siendo arbitrada por `descontarCupo` al comprar.
- Después del fix, al ingresar con una tanda `ABIERTA` vigente y con cupo, el
  cliente se habilita **en ese mismo momento** si quedan lugares en el lote y
  nadie con secuencia menor está esperando. El lote se llena **en orden de
  llegada**, dentro del lock de evento, sin depender de un pago ajeno que
  cierre y abra otra tanda.
- No hace falta sobredimensionar `maxPersonas` para una prueba de carga. Con
  `maxPersonas=30` y `ramp=0` se obtuvieron **30/30** compras OK y 0 errores;
  con `ramp=10`, también **30/30** (ver `resultados/carga.md`). Si la demanda
  supera el lote, el excedente queda `EN_ESPERA` conservando su secuencia y se
  habilita en la tanda siguiente; si supera el cupo, recibe el 409 de cupo
  existente. En ningún caso se vende por encima del cupo.
- La corrida de 45 clientes con `maxPersonas=30` confirmó el tope: **30** en la
  primera tanda y **15** `EN_ESPERA`, con 0 inversiones de secuencia en base.
  Esos 15 se habilitaron ~6 s después al cerrarse la tanda (ver
  `resultados/carga-45.md`). Ese mismo documento aclara que `cupoEntradas` es
  cupo **por tanda**: con 1000 lugares disponibles se vendieron 90 entradas en
  dos tandas, sin sobreventa. Para un tope global hace falta que la
  disponibilidad del sector sea el límite; no es un efecto de `maxPersonas`.

## Seguimiento separado

La corrección de la regla en el documento archivado queda como seguimiento
aparte para no mezclarla con este change ni con la deuda preexistente de
`## Purpose`. La sincronización del delta `fila-virtual` a la spec viva, en
cambio, se hizo dentro de este change antes de archivarlo, siguiendo el flujo de
OpenSpec.

Verificación: `git diff --name-only` lista seis borrados bajo
`openspec/changes/verificacion-e2e-carga/`, pero son preexistentes al cambio de
nombre de ese directorio a `openspec/changes/archive/2026-11-23-verificacion-e2e-carga`.
Este change no editó ninguno de esos archivos: el contenido del change
archivado permanece intacto bajo `archive/`.
