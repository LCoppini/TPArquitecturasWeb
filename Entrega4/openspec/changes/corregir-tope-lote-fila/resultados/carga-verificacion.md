# Verificación de carga tras el fix del tope

- Fecha: 2026-11-23.
- Evento **21**, mismo escenario de la corrida A de `turno-vigente-al-ingresar`:
  45 clientes, `ramp=0`, 2 entradas por compra, `maxPersonas=30`,
  `cupoEntradas=60`, sector 1 de `capacidadMax=1000`, 2 réplicas de
  `msvc-entrada` con el artefacto rebuildado (`entrega4-msvc-entrada:latest`,
  ambas réplicas `healthy`), gateway en `127.0.0.1:8080`.
- Timeout de polling: `-JpollTimeoutMs=60000` (el mismo de la corrida 45/30).
- JTL XML: `jtl/ramp0-45-tope.jtl`. Informe HTML generado desde el JTL:
  `informe-jmeter.html` (gemelo Markdown: `informe-jmeter.md`). Sin `jmeter.log` versionado.

## Resultado del sampler

```
ESCENARIO r45m30top id=21 threads=45 max=30 cupo=60 ramp=0 timeout=60000
  muestras=250 ingresos=45 getEstado=114 compras=45 ok=45 pagos=45 ok=45 timeouts=0 fallos=0 p95compra=1887ms
```

## Verificación en base

```sql
select t.numero, count(a.id), min(a.secuencia), max(a.secuencia)
from tanda_fila t join entrada_fila a on a.id_tanda=t.id and a.id_evento=t.id_evento
where t.id_evento=21 group by t.numero order by t.numero;
-- 1 | 30 | 1  | 30
-- 2 | 15 | 31 | 45

select count(*) from entrada_fila a
where a.id_evento=21 and exists (
  select 1 from entrada_fila b
  where b.id_evento=a.id_evento and b.secuencia<a.secuencia and b.id_tanda>a.id_tanda
);
-- 0 inversiones FIFO
```

| Comprobación | Valor |
|---|---:|
| Compras OK / pagadas | 45 / 45 |
| Claves de idempotencia de compra | 45 distintas |
| Claves de idempotencia de pago | 45 distintas |
| Entradas `VALIDA` | 90 |
| Sobreventa | 0 (90 ≤ 1000) |
| Timeouts / fallos | 0 / 0 |
| Tanda 1 / tanda 2 | 30 / 15 personas |
| Inversiones FIFO | 0 |

Los 15 excedentes de la primera tanda volvieron a quedar `EN_ESPERA` y se
habilitaron al cerrarse la tanda 1, igual que en la corrida A original. El fix
no cambió el diseño de tandas: `cupoEntradas` sigue siendo por tanda, por lo que
la tanda 2 abrió con 60 de cupo y se emitieron 90 entradas en total. Lo que cambia
es quién ocupa el lote cuando una membresía ya pasó a `COMPRO` o `VENCIDO`.
