# Resultado después del fix (verde)

- Fecha: 2026-11-23.
- Comando 1: `mvn -pl msvc-entrada test -Dtest=CompraConcurrenciaIntegrationTest -DfailIfNoTests=false` → `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- Comando 2: `mvn -pl msvc-entrada test` → `Tests run: 205, Failures: 0, Errors: 0, Skipped: 6`, `BUILD SUCCESS`. Los 6 skipped son preexistentes (tests de outbox/preferencias condicionados).
- Alcance verificado con `git diff`: solo `FilaVirtualService.java` (28 líneas) y `CompraConcurrenciaIntegrationTest.java` (196 líneas nuevas). `CompraService.java`, `TandaFilaRepository.java`, `msvc-config/`, `docker-compose.yml` y recursos de `msvc-entrada` sin cambios.
- No se agregó configuración: la decisión del grupo fue no incorporar el mínimo de tiempo restante de la tanda.
- Logs completos en `C:\Users\lauty\AppData\Local\Temp\opencode\diag\mvn-verde.log` y `mvn-suite.log` (fuera del repo).
