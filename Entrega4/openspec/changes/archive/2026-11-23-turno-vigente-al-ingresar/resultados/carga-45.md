# Corrida con demanda mayor que el lote (45 clientes, `maxPersonas=30`)

- Fecha: 2026-11-23.
- 45 clientes concurrentes, `ramp=0`, 2 entradas por compra, `maxPersonas=30`,
  `cupoEntradas=60`, 2 réplicas de `msvc-entrada`, gateway en `127.0.0.1:8080`.
- Plan: `../jmeter/prueba-carga.jmx` (polling hasta `HABILITADO`, pago solo si
  hay `idCompra`). El plan parametriza `idEvento`, `idSector`, `runId`, hilos,
  ramp, `maxPersonas`, `cupoEntradas` y **timeout de polling**.
- **Timeout usado: `-JpollTimeoutMs=60000`** (60 s), subido del default de
  15 s. Quien excede el lote debe esperar a que se cierre la tanda anterior o a
  que aparezca disponibilidad; con 15 s los últimos habrían marcado timeout
  antes de tiempo. Quedó documentado en `../jmeter/README.md`. No se cambió el
  código de producción.
- JTL en XML: `jtl/ramp0-45.jtl` y `jtl/ramp0-45-cupo60.jtl`. Sin HTML ni
  `jmeter.log` versionados (ADR-0002).

## Corrida A — configuración pedida, sector con 1000 lugares

Evento **16**, clientes `r45m30_01` … `r45m30_45`, sector 1 (`capacidadMax=1000`).

| Métrica | Valor |
|---|---:|
| `POST /ingresos` | 45 |
| `GET /mi-estado` (polling) | 130 |
| Compras OK | **45/45** |
| Pagos OK | 45/45 |
| Timeouts esperando `HABILITADO` | 0 |
| Muestras fallidas | 0 |
| p95 compra (30 muestras) | 1150 ms |

**Clientes sin comprar: 0.** `intento_compra` está vacío y el JTL no contiene
bodies de error: no hay 409 de turno, ni `CUPO_TANDA_INSUFICIENTE`, ni timeouts.
Por lo tanto no hay mensajes literales de rechazo que reportar en esta corrida.

### Tope y FIFO

| Tanda (id) | Rango de secuencias | Cantidad | Estado final |
|---|---|---:|---|
| 154 (1) | 1 – 30 | **30** | `COMPRO` |
| 155 (2) | 31 – 45 | 15 | `COMPRO` |

- Habilitados en la primera tanda: **30 = `maxPersonas`**. Nunca más.
- Los 15 restantes arrancaron `EN_ESPERA` y entraron en la tanda 2 al cerrarse
  la primera (19:43:38.921 UTC; últimas llegadas 19:43:34.165 UTC → ~6,2 s de
  espera, muy por debajo de los 60 s de timeout).
- Estado final: **0 `EN_ESPERA`**, 45 `COMPRO`. Nadie quedó esperando con cupo
  disponible.
- SQL de FIFO:

```sql
select count(*) from entrada_fila a
where a.id_evento = 16
  and exists (
    select 1 from entrada_fila b
    where b.id_evento = a.id_evento
      and b.secuencia < a.secuencia
      and b.id_tanda > a.id_tanda
  );
-- 0 inversiones
```

Los rangos son contiguos (1-30 y 31-45) y `ingresado_en` crece con la secuencia
(1 → 19:43:32.688 UTC, 45 → 19:43:34.165 UTC): sin adelantamientos.

### Hallazgo: `cupoEntradas` es por tanda, no global

Se vendieron **90 entradas**, no 60. `abrirTandaSiCorresponde`
(`FilaVirtualService.java:360`) calcula, para cada tanda,
`cupoEfectivo = min(cupoEntradas + remanente, disponibles)`:

| Tanda | `cupo_efectivo` | `cupo_restante` | Consumido |
|---|---:|---:|---:|
| 1 | 60 | 0 | 60 |
| 2 | 60 | 30 | 30 |

Al cerrarse la tanda 1 se abrió otra con 60 de cupo, no con el remanente
global. No hay sobreventa: 60 + 30 = 90 ≤ 1000 disponibles y nunca se descontó
más de `cupo_restante`. Si se quisiera un tope global de 60 entradas para todo
el evento, sería un cambio de diseño de tandas, fuera de este change y **no lo
implementé sin tu decisión**.

## Corrida B (control) — tope global real de 60 entradas

Para verificar el criterio “demanda 90 contra cupo 60” con un límite global, se
repitió el escenario con un lugar nuevo de **`capacidadMax=60`** (sector 4).
Sigue siendo configuración de datos: mismos 45 hilos, `ramp=0`,
`maxPersonas=30`, `cupoEntradas=60`, mismo sampler y timeout de 60 s. Evento
**20**.

| Métrica | Valor |
|---|---:|
| `POST /ingresos` | 45 |
| Compras OK | **30/30** |
| Pagos OK | 30/30 |
| Timeouts esperando `HABILITADO` | **15** |
| Muestras fallidas | 15 (las mismas 15) |
| Entradas vendidas | **60 = cupo** |
| p95 compra (30 muestras) | 1448 ms |

### Clientes sin comprar

- **15 clientes**, los de secuencias **31 a 45**. Causa raíz: cupo global
  agotado; `fila_evento` quedó `AGOTADA` y `tanda_fila.cupo_restante = 0`, así
  que no se abrió una tanda 2 que los habilitara.
- Como el sampler no compra sin `HABILITADO`, no hay un 409 HTTP de cupo que
  correlacionar: los 15 terminan en el sampler de timeout. Mensaje literal,
  15/15 idéntico (definido en `prueba-carga.jmx:238`):

  > `No se habilitó el turno antes del timeout; estado=EN_ESPERA`

- `intento_compra` está vacío: ningún cliente en espera llegó a `POST /compras`.
  No hay rechazos por turno inexistente (409 `No hay un turno vigente...`): 0.

### Tope y FIFO

| Grupo | Secuencias | Cantidad | Estado final |
|---|---|---:|---|
| Tanda 159 | 1 – 30 | **30 = `maxPersonas`** | `COMPRO` |
| Sin tanda | 31 – 45 | 15 | `EN_ESPERA` |

```sql
select count(*) from entrada_fila a
where a.id_evento = 20
  and exists (
    select 1 from entrada_fila b
    where b.id_evento = a.id_evento
      and b.secuencia < a.secuencia
      and coalesce(b.id_tanda, 999999) > coalesce(a.id_tanda, 999999)
  );
-- 0 inversiones
```

Los 15 que esperan tienen cupo **0** disponible (tanda 1 cerrada con
`cupo_restante=0`, fila `AGOTADA`), así que no hay nadie `EN_ESPERA` con cupo
disponible. En la corrida A, en cambio, los 15 esperaron ~6 s y compraron.

### Integridad

| Comprobación | Corrida A | Corrida B |
|---|---:|---:|
| Compras pagadas | 45 / 45 | 30 / 30 |
| Claves de idempotencia distintas | 45 | 30 |
| Entradas emitidas | 90 | 60 |
| Compras distintas con entrada | 45 | 30 |
| Intentos rechazados (`intento_compra`) | 0 | 0 |
| Sobreventa | 0 | 0 |

## Conclusión

- Tope de `maxPersonas` y FIFO: verificados bajo carga real en ambas corridas
  (30 habilitados de 45, secuencias 1-30; 0 inversiones).
- 0 sobreventa y 0 rechazos por turno inexistente en las dos corridas.
- Ningún cliente quedó `EN_ESPERA` con cupo disponible.
- El único punto que no coincide con el enunciado es el total de entradas de la
  corrida A (90 > 60), explicado por el cupo por tanda; la corrida B muestra que
  con un tope global de 60 el sistema vende exactamente 60 y deja a 15 en espera
  por cupo agotado.

## Informe HTML

No se versiona (ADR-0002). El JTL XML se convirtió a CSV solo en
`C:\Users\lauty\AppData\Local\Temp\opencode\diag\` y el dashboard se regeneró en
local. Ni HTML, ni `jmeter.log`, ni tokens quedaron en el repo.
