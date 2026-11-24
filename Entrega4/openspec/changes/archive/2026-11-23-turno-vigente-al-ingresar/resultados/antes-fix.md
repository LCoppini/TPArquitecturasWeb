# Resultado antes del fix (rojo)

- Fecha: 2026-11-23.
- Comando: `mvn -pl msvc-entrada test -Dtest=CompraConcurrenciaIntegrationTest -DfailIfNoTests=false` (Maven bundled de IntelliJ 2026.1).
- Estado: **BUILD FAILURE**. `Tests run: 43, Failures: 2, Errors: 2, Skipped: 0`.
- Los 39 tests preexistentes de la clase siguen en verde; fallan exactamente los 4 tests nuevos.

| Test | Fallo observado |
|---|---|
| `nConcurrentesSobreCapacidadNCompranTodosSinRechazo` | `ExecutionException: 409 CONFLICT "No hay un turno vigente para iniciar la Compra"`. Ninguno de los 8 clientes obtuvo turno en la tanda abierta. |
| `doceIngresosConcurrentesRespetanTopeMaxPersonas` | `expected: <8> but was: <0>`: 0 `HABILITADO`, 12 `EN_ESPERA`. |
| `habilitacionRespetaOrdenDeSecuenciaYNoAdelantaAQuienEsperaba` | `expected: <[1, 2]> but was: <[]>`: 0 habilitados; no se puede verificar el orden de secuencias. |
| `demandaMayorAlCupoVendeExactamenteCupoEntradas` | `ExecutionException: 409 CONFLICT "No hay un turno vigente para iniciar la Compra"`. 0 entradas vendidas. |

Conclusión: los tests reproducen la causa raíz aprobada sin depender del sampler JMeter ni de datos de diagnóstico externos. El log completo quedó en `C:\Users\lauty\AppData\Local\Temp\opencode\diag\mvn-rojo.log` (fuera del repo).
