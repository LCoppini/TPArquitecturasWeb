# Plan JMeter corregido

`prueba-carga.jmx` deriva del plan archivado de `verificacion-e2e-carga`, con
tres correcciones:

1. Después de `POST /ingresos`, hace polling de `GET /mi-estado` hasta que
   `estado == HABILITADO`. El plazo se controla con `-JpollTimeoutMs` (default
   15000) y el reintento es cada 200 ms. Si vence, el sampler
   `FALLO - timeout esperando HABILITADO` se marca como fallido (código 599)
   y el hilo no compra, sin abortar el plan.
2. El pago está dentro de un `IfController` que solo lo ejecuta si la compra
   devolvió `idCompra`. Ya no se envía `SIN_ID` ni se contabiliza un 400 de pago
   como error derivado.
3. `idEvento`, `idSector`, `runId`, `maxPersonas` y `cupoEntradas` son properties
   del plan, no literales.

## Properties

| Property | Default | Uso |
|---|---|---|
| `-JorgToken` | — | JWT del organizador (nunca versionar). |
| `-JclientTokens` | `clientes_carga.csv` | CSV local con cabecera `token,cliente`. |
| `-JidEvento` | `2` | Evento de la corrida (variable `idEvento` del plan). |
| `-JidSector` | `1` | Sector con precio configurado en ese evento. |
| `-JrunId` | timestamp `yyyyMMddHHmmss` | Prefijo de las claves de idempotencia; `carga-<runId>-u<hilo>-compra/pago`. |
| `-Jthreads` | `30` | Clientes concurrentes. |
| `-Jramp` | `10` | Ramp-up en segundos (usar `0` y `10` en las corridas). |
| `-JmaxPersonas` | `30` | Tamaño del lote de habilitación. |
| `-JcupoEntradas` | `60` | Cupo de entradas de la tanda. |
| `-JpollTimeoutMs` | `15000` | Timeout del polling a `HABILITADO`. Usar `60000` cuando la demanda supera `maxPersonas` (ver `../resultados/carga-45.md`). |

## Ejemplo

```powershell
& 'C:\Users\lauty\AppData\Local\Programs\JMeter\bin\jmeter.bat' -n `
  -t 'jmeter\prueba-carga.jmx' -l 'resultados\jtl\ramp0.jtl' `
  '-JidEvento=17' '-JorgToken=<token-local>' `
  '-JclientTokens=C:\Users\lauty\AppData\Local\Temp\opencode\diag\cli_fix_ramp0.csv' `
  -Jthreads=30 -Jramp=0 -JmaxPersonas=30 -JcupoEntradas=60
```

Para ver el bodies de errores en el JTL:

```powershell
-Jjmeter.save.saveservice.output_format=xml -Jjmeter.save.saveservice.response_data.on_error=true
```

## Informe HTML (solo local, ADR-0002)

No se versiona. Los JTL se guardan en XML para conservar los bodies de error;
JMeter 5.6.3 no genera el dashboard directamente desde XML (`Could not read
sample <0>`), así que se convierten a CSV solo en temp y se regenera desde ahí:

```powershell
& $jmeter -g 'C:\Users\lauty\AppData\Local\Temp\opencode\diag\ramp0-report.csv' `
  -o 'C:\Users\lauty\AppData\Local\Temp\opencode\diag\informe-ramp0'
```

Los informes de ambas corridas quedaron en
`C:\Users\lauty\AppData\Local\Temp\opencode\diag\informe-*` y no se copiaron al
repo. `jmeter.log` contiene el token pasado por propiedad: queda en el mismo
directorio temporal y nunca se copia al repo.
