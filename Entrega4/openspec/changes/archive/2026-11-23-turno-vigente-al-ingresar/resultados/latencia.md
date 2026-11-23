# Análisis de la latencia con `ramp=0`

## Pregunta

¿Por qué el p95 de `POST /compras` fue 1926 ms con 30 clientes y `ramp=0`,
contra 192 ms con `ramp=10`, sin cambios de código?

## Respuesta corta

**Hipótesis confirmada, con un matiz.** Hay serialización por el bloqueo de
evento, pero el punto de contención no es el advisory *compartido* en sí (dos
advisory shared son compatibles), sino el `SELECT ... FOR UPDATE` sobre la fila
de `fila_evento` que se toma inmediatamente después, tanto en `unirse` como en
`crear`. Con `ramp=0` las 30 peticiones llegan en ~21 ms y forman una cola
detrás de ese lock; con `ramp=10` llegan cada ~333 ms y el lock está libre cuando
llega la siguiente.

Certeza: **alta** sobre la causa (gradiente por orden de llegada reproducible
en tres corridas + evidencia de código); **media** sobre la contribución exacta
de ruido de JVM/cliente a los 1926 ms (no hay logs de GC).

## Evidencia de código

| Punto | Ubicación | Lock |
|---|---|---|
| `unirse` | `FilaVirtualService.java:158-159` | `bloquearEventoCompartido` + `buscarFilaConBloqueo` |
| advisory compartido | `FilaEventoRepository.java:23-30` | `pg_advisory_xact_lock_shared(1346454345, idEvento)` |
| fila de evento | `FilaEventoRepository.java:38-40` | `@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE` |
| `crear` compra | `CompraService.java:170,177` | mismo advisory compartido + mismo `FOR UPDATE` de fila |
| apertura/cierre de tanda | `TandaFilaRepository.java:16-21` | `PESSIMISTIC_WRITE` sobre la fila de tanda |

Todas esas adquisiciones viven en la misma transacción de cada request, así que
la transacción que llega primero mantiene la fila bloqueada hasta hacer commit;
las demás esperan. El lock de idempotencia (`CompraService.java:171`) usa claves
distintas por cliente, por lo que **no** es la causa de la cola.

## Distribución por orden de llegada

Calculado desde los JTL XML ordenando por `ts` (inicio de la muestra), con
correlación de Pearson entre orden y latencia:

| Corrida | Etapa | n | min | med | p95 | max | r(orden, latencia) | media 1.ª mitad → 2.ª |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| `ramp0` original | ingreso | 30 | 287 | 1330 | 2035 | 2060 | 0,633 | 1001 → 1633 ms |
| `ramp0` original | compra | 30 | 572 | 1285 | 1926 | 1975 | **0,670** | **1068 → 1551 ms** |
| `ramp0` repetición 1 | compra | 30 | 159 | 812 | 1392 | 1423 | 0,568 | 651 → 963 ms |
| `ramp0` repetición 2 | compra | 30 | 157 | 756 | 1280 | 1321 | 0,718 | 471 → 1010 ms |
| `ramp10` | ingreso | 30 | 40 | 64 | 131 | 149 | 0,029 | 67 → 74 ms |
| `ramp10` | compra | 30 | 47 | 73 | 192 | 198 | **−0,424** | **102 → 81 ms** |

En las tres corridas con `ramp=0` la latencia sube con el orden de llegada
(correlaciones 0,57–0,72 y segunda mitad siempre peor). Con `ramp=10` la
correlación desaparece o se invierte y la segunda mitad es incluso más rápida.
Eso es exactamente el patrón de una cola detrás de un recurso serializado, no de
un problema global del servicio.

Los tiempos de llegada refuerzan lo anterior: en `ramp0` las 30 muestras de
ingreso arrancan en un intervalo de ~21 ms; en `ramp10`, en ~8 s.

## Repetición y variación del p95

| Corrida | Inicio UTC | p95 compra |
|---|---|---:|
| `ramp0` original (evento 14) | 19:26:00 | 1926 ms |
| `ramp0` repetición 1 (evento 17) | 19:44:11 | 1392 ms |
| `ramp0` repetición 2 (evento 18) | 19:44:26 | 1280 ms |

El valor absoluto varía ~34 % entre repeticiones (ruido del host, JVM, gateway),
pero el **gradiente por orden** se mantiene. Por eso la causa es estable y la
cifra puntual de 1926 ms no debe leerse como un número exacto.

## Cuánto vale cada p95

p95 calculado con nearest-rank sobre 30 muestras = muestra 29 de 30.

| Corrida | p95 | Muestras ≥ p95 | Vecinos inmediatos |
|---|---:|---:|---|
| `ramp0` compra | 1926 ms | **2** (1926, 1975) | 1897 ms antes |
| `ramp10` compra | 192 ms | **2** (192, 198) | 171 ms antes |

Solo dos muestras sostienen cada cifra: mover una sola muestra cambia el p95.
La diferencia 1926 vs 192 es de ~10×, mucho mayor que esa fragilidad, así que
la comparación es válida; el valor puntual no.

## Causas alternativas evaluadas

- **HikariCP / pool agotado:** descartado con la evidencia disponible. Los logs
  de las dos réplicas muestran `HikariPool-1 - Start completed` a las
  19:24:29/19:24:30 UTC, ~90 s antes de la corrida original, y no aparecen
  `Connection is not available`, timeouts de adquisición ni errores de pool en
  ninguna de las corridas.
- **Arranque en frío:** descartado. La corrida original empieza a las 19:26,
  después de que las réplicas estuvieran sanas varios minutos; las repeticiones
  de las 19:44 (18 minutos después) reproducen el mismo gradiente, solo que con
  menor magnitud.
- **GC / pausas de JVM:** no se pueden cuantificar (no hay `-Xlog:gc`), y la
  ausencia de líneas de GC en los logs de Docker no es prueba definitiva. Pero
  una pausa afectaría a todas las peticiones simultáneas por igual; no explicaría
  una correlación positiva sostenida con el orden de llegada en tres corridas ni
  que desaparezca con `ramp=10`.
- **Cliente JMeter / gateway:** puede aportar sobrecarga cuando 30 hilos arrancan
  a la vez, pero no explica el gradiente *dentro* de la misma ventana de tiempo
  ni que `GET /mi-estado` y `POST /ingresos` muestren el mismo patrón.

## Consecuencia operativa

El fix no cambia este comportamiento: la habilitación directa ocurre dentro del
mismo lock de evento. El aspecto crítico era que ese lock entregara turnos
útiles a todos los que llegan; una vez logrado, la serialización restante es el
coste de mantener secuencia única y cupo atómico con varias réplicas. El
`ramp-up` sigue siendo una paleta del generador de carga, no una regla de
dimensionamiento (ver `nota-maxpersonas.md`). No se modificó código por este
análisis.
