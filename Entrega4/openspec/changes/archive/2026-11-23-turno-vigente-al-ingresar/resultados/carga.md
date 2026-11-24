# Prueba de carga tras el fix (2 réplicas de `msvc-entrada`)

- Fecha: 2026-11-23.
- Entorno: compose activo de `Entrega4`, 11 contenedores en marcha, gateway en
  `127.0.0.1:8080`, dos instancias registradas de `msvc-entrada`. El plan se
  ejecutó con el fix ya reconstruido en ambas réplicas; no se cambió compose ni
  configuración versionada.
- Herramienta: Apache JMeter 5.6.3. Plan: `../jmeter/prueba-carga.jmx`
  (polling hasta `HABILITADO`, pago condicionado a `idCompra`).
- Datos crudos: `jtl/ramp0.jtl` y `jtl/ramp10.jtl` (XML, sin `jmeter.log`).
- Cada corrida usó clientes, evento y fila nuevos: evento **14** (`ramp0`) y
  evento **15** (`ramp10`). Tokens y CSV quedaron solo en
  `C:\Users\lauty\AppData\Local\Temp\opencode\diag\`.

## Parámetros comunes

30 clientes, 30 hilos, 2 entradas por compra del sector 1,
`maxPersonas=30`, `cupoEntradas=60`, `pollTimeoutMs=15000`. Única diferencia:
`ramp=0` vs `ramp=10` segundos.

Nota de trazabilidad: el plan versionado toma `idEvento` por property; las
corridas usaron una copia temporal equivalente con el id fijado. Después de las
corridas se reforzó el sampler de timeout para que registre una muestra fallida
sin abortar JMeter (validado con una corrida en seco); no afectó las métricas
porque no se produjo ningún timeout.

## Métricas

Tiempos en milisegundos. p95 por nearest-rank sobre 30 muestras. Errores =
muestras con `s=false` (incluye timeout de habilitación y compra fallida).

| Corrida | Evento | Muestras | Ingresos | `GET mi-estado` | Compras OK | Pagos OK | Timeouts | Errores | p95 compra | p95 pago | p95 ingreso | Duración |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `ramp0` | 14 | 121 | 30 | 30 | **30/30** | 30/30 | 0 | **0** | 1926 | 2118 | 2035 | ~5,0 s |
| `ramp10` | 15 | 121 | 30 | 30 | **30/30** | 30/30 | 0 | **0** | 192 | 230 | 131 | ~10,1 s |

Detalle por transacción:

| Transacción | ramp0 n / min / med / p95 / máx | ramp10 n / min / med / p95 / máx |
|---|---|---|
| PUT activar fila | 1 / 429 / 429 / 429 / 429 | 1 / 77 / 77 / 77 / 77 |
| 1. POST ingreso a fila | 30 / 287 / 1330 / 2035 / 2060 | 30 / 40 / 64 / 131 / 149 |
| 2a. GET mi-estado | 30 / 43 / 105 / 201 / 210 | 30 / 10 / 16 / 48 / 49 |
| 3. POST compra (2 entradas) | 30 / 572 / 1285 / 1926 / 1975 | 30 / 47 / 73 / 192 / 198 |
| 4. POST pago | 30 / 1262 / 1740 / 2118 / 2143 | 30 / 47 / 64 / 230 / 257 |

**Conteo de errores por mensaje: ninguno.** No hubo 409 `No hay un turno
vigente`, ni `CUPO_TANDA_INSUFICIENTE`, ni timeouts de habilitación, ni pagos
`SIN_ID` (0 apariciones en ambos JTL). Con `ramp=0`, la latencia sube por la
serialización del lock de evento, pero las 30 compras se completan.

## Integridad (anti doble venta)

Consultas sobre `ticketya_entrada` después de cada corrida:

| Comprobación | Evento 14 (`ramp0`) | Evento 15 (`ramp10`) |
|---|---:|---:|
| Membresías `COMPRO` / total | 30 / 30 | 30 / 30 |
| Compras `PAGADA` / total | 30 / 30 | 30 / 30 |
| Claves de idempotencia de compra distintas | 30 | 30 |
| Entradas emitidas | 60 | 60 |
| Compras distintas con entrada | 30 | 30 |
| `intento_compra` rechazados | 0 | 0 |
| Entradas por encima del cupo vendido | 0 | 0 |

En el evento 14 la tanda 1 cerró con `cupo_efectivo=60` y
`cupo_restante=0`: se vendió exactamente el cupo. En el evento 15 los pagos
sucesivos dispararon varias tandas (1 a 7), todas cerradas, con cupo sobrante
real; las 60 entradas emitidas corresponden a 30 compras × 2 y ningún cliente
quedó `EN_ESPERA` al finalizar. No se modificó `descontarCupo` ni la reserva
atómica.

## Comparación con el diagnóstico pre-fix

| Escenario | Compras OK |
|---|---:|
| Pre-fix, `ramp=10`, `maxPersonas=30` | 30/30 |
| Pre-fix, `ramp=0`, `maxPersonas=30` | 10/30 |
| Pre-fix, `ramp=0`, `maxPersonas=60` | 4/30 |
| Post-fix, `ramp=0`, `maxPersonas=30` | **30/30** |
| Post-fix, `ramp=10`, `maxPersonas=30` | **30/30** |

El sampler corregido deja de enmascarar el defecto: espera a `HABILITADO` en
vez de asumir que el turno ya está listo, y aun con `ramp=0` el lote se llena
en orden de llegada. Subir `maxPersonas` ya no es necesario; ver
`../nota-maxpersonas.md`.

## Informe HTML (solo local)

Los JTL se guardan en XML para conservar bodies de error. JMeter 5.6.3 no
genera el dashboard directamente desde XML, así que se convirtieron a CSV en
temp y se regeneraron los informes en
`C:\Users\lauty\AppData\Local\Temp\opencode\diag\informe-ramp0` e
`informe-ramp10` (ambos con `index.html`). Ni los informes, ni `jmeter.log`, ni
tokens se versionaron.
