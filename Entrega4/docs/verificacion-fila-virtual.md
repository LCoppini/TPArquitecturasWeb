# Verificación del change `fila-virtual`

Fecha: 2026-11-22. Alcance: cerrar la verificación del change sin archivarlo y dejar `tasks.md` con el estado real de implementación + evidencia.

## Entorno

- Java 21.0.10; Maven 3.9.16 (wrapper local `~/.m2/wrapper/dists/apache-maven-3.9.16/.../bin/mvn.cmd`).
- Docker Desktop activo, `Docker version 29.7.2`; `docker compose ps` muestra `ticketya-postgres` (`postgres:16`, puerto 5432).
- Pruebas con Testcontainers `postgres:16` (16.15) para carreras y con el PostgreSQL del compose para las pruebas `ENTRADA_DB_INTEGRATION`.

## Comandos ejecutados y resultados

| Comando (desde `Entrega4`) | Resultado |
|---|---|
| `openspec validate "fila-virtual"` | `Change 'fila-virtual' is valid`. |
| `mvn -B -pl msvc-entrada,msvc-gateway -am test` | `msvc-gateway` 76/76; matriz de roles 50 tests OK. |
| `mvn -B -pl msvc-entrada test` (sin Docker) | 72 tests, 0 failures, 0 errors, 6 skipped, `BUILD SUCCESS`. |
| `mvn -B -pl msvc-entrada test` (con Docker/Testcontainers) | 90 tests, 0 failures, 0 errors, 6 skipped, `BUILD SUCCESS`. El test nuevo de eventos se ajustó después a seis entradas y se reejecutó aparte. |
| `mvn -B -pl msvc-entrada -Dtest=CompraEventoPublicacionTest test` | 1 test, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`: un solo evento agregado con seis IDs de entrada y el `correlationId`. |
| `$env:ENTRADA_DB_INTEGRATION='true'; mvn -B -pl msvc-entrada '-Dtest=OutboxCorrelationEndToEndTest,PagoOutboxRollbackIntegrationTest,PendientePreferenciaClaimIntegrationTest' test` | 6 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`. Cubre correlación con dos contextos de aplicación, rollback del outbox de pago y reclamo concurrente de preferencias con `FOR UPDATE SKIP LOCKED`. |
| `mvn -B -pl msvc-entrada '-Dtest=IngresoFilaTest,CompraCancelacionMotivoTest,CompraPagoFalloTest,CompraEventoPublicacionTest' test` | 5 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`. Cubre secuencia inmutable, motivo de cancelación preservado en reintegro/reintento, compensación por `FALLA` y evento agregado de seis entradas. |
| `mvn -B -pl msvc-evento '-Dtest=EntradaClientContratoTest' test` | 1 test, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`. Fija la ruta administrativa de cancelación en `EntradaClient`. |
| `mvn -B -pl msvc-entrada '-Dtest=CancelacionVerificacionIntegrationTest' test` | 6 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`. Además de la compensación por `FALLA`, el motivo `CANCELACION` y la unicidad de secuencia, cubre entrada `USADA` con reintegro total único, carrera pago-cancelación y una sola ganadora en la carrera de resultados. |
| `mvn -B -pl msvc-evento '-Dtest=AdminEventoControllerCancelacionTest,EntradaClientContratoTest,EventoServiceImplTest' test` | 24 tests, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`. Fija la ruta y el header `X-User-Id` del `EntradaClient`, la ruta administrativa de reintento, su delegación y la reinvocación del cliente desde `EventoServiceImpl`. |
| `mvn -B -pl msvc-entrada,msvc-evento -am test` | `msvc-evento` y `msvc-entrada` en verde; 102 tests en Entrada y 53 en Evento (155 en total), 0 failures, 0 errors, 6 skipped; `BUILD SUCCESS`. Reejecutada tras todas las correcciones y los tests nuevos de esta ronda. |
| `mvn -B -pl msvc-entrada,msvc-gateway,msvc-evento -am test` (tras 6.2/3.3) | 337 tests, 0 failures, 0 errors, 6 skipped; `BUILD SUCCESS`. |
| `mvn -B -pl msvc-entrada,msvc-gateway,msvc-evento -am test` (tras 6.1) | 340 tests, 0 failures, 0 errors, 6 skipped; `BUILD SUCCESS`. `GatewayErrorHandlingTest` quedó en 14 tests. |
| `mvn -B -pl msvc-entrada,msvc-gateway '-Dtest=DocumentacionApiTest,DocumentacionApiGatewayTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` | 4 tests en Entrada y 2 en Gateway, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. Contrasta documentación, anotaciones OpenAPI y prefijos ruteados. |
| `mvn -B -pl msvc-entrada '-Dtest=CompraConcurrenciaIntegrationTest' test` (tras el escenario de rollback) | 39 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. |
| `mvn -B -pl msvc-entrada,msvc-gateway,msvc-evento -am test` (final) | 347 tests (93 gateway + 53 evento + 201 entrada), 0 failures, 0 errors, 6 skipped; `BUILD SUCCESS`. |

Los seis tests omitidos en la suite estándar están detrás de `@EnabledIfEnvironmentVariable(named = "ENTRADA_DB_INTEGRATION", matches = "true")` y usan el PostgreSQL del compose; se ejecutaron explícitamente con el comando anterior.

## Correcciones aplicadas durante la verificación

1. `EntradaService` usa `ObjectProvider<CompraService>` para eliminar `No qualifying bean of type 'CompraService'` en los cortes de contexto legacy.
2. `CompraService.pagar` limpia el `EntityManager` antes de tomar locks; desaparecieron los `ObjectOptimisticLockingFailureException` en `Compra#1`/`Compra#6`.
3. `CompraPagadaEvent` se publica solo en la rama aprobada del pago; `CompraEventoPublicacionTest` verifica un único evento con los seis IDs y el `correlationId`.
4. Los caminos de escritura (pago, vencimiento, cierre y cancelación de tanda) pasan de lock compartido a exclusivo.
5. `PagoCompraResponse.estado` informa el estado agregado de la Compra (`RECHAZADA`), distinto del estado interno del Pago (`RECHAZADO`), según el criterio acordado. `design.md` lo documenta.
6. `docs/eventos.md` documenta deduplicación por `idCompra`/`idEntrada` y emisión post-commit con el mismo `correlationId`.
7. En esta verificación no se modificaron tests existentes para forzar que pasaran; se agregaron pruebas nuevas. No se archivó ni commiteó el change.
8. `msvc-evento.entrada.client.EntradaClient.cancelarEntradas` apunta ahora a `POST /api/admin/entradas/eventos/{idEvento}/cancelacion`; `design.md` y la tarea 3.5 quedaron alineados.
9. `IngresoFila.setSecuencia` rechaza un cambio de secuencia una vez asignada (2.1).
10. `CompraService.ejecutarReintegroCompleto` conserva un `motivoInvalidacion` ya existente, de modo que `CANCELACION` no se convierte en `DEVOLUCION` durante el reintegro ni su reintento (3.5).
11. El resultado `FALLA` de la pasarela en el pago agrupado marca la Compra como `RECHAZADA`, el Pago como `FALLIDO`, cancela las entradas, devuelve cupo y no publica `CompraPagadaEvent`, permitiendo un nuevo intento (5.3). `DEMORA` permanece `PENDIENTE` deliberadamente: el resultado es indeterminado y no debe liberarse el turno.
12. Se agregaron `CancelacionVerificacionIntegrationTest` (PostgreSQL/Testcontainers) y `AdminEntradaControllerCancelacionTest` (MockMvc) para verificar los caminos anteriores contra base real y la ruta HTTP canónica, sin modificar tests existentes.
13. Se amplió la evidencia de cierre sin tocar producción: `CancelacionVerificacionIntegrationTest` incorpora entrada `USADA`, reintegro total único, reintento sin duplicarlo, carrera pago-cancelación y carrera de resultados con un solo ganador; `AdminEventoControllerCancelacionTest` fija `POST /api/admin/eventos/{id}/cancelacion/reintentar` y `EventoServiceImplTest` verifica que el reintento vuelve a invocar `EntradaClient`.
14. 3.3 se cerró con 12 iteraciones de cierre concurrente y creación de Compra: en cada una la tanda queda `CERRADA` y solo ocurre una de dos salidas linealizables (Compra `RESERVADA` en la tanda abierta con una Entrada y cupo descontado, o 409 sin Compra ni Entradas y cupo intacto).
15. 6.2 agregó `ticketya.fila-virtual.enabled` (variable `FILA_VIRTUAL_ENABLED`, default `true`) en `application.yml` y en la configuración centralizada; con `false` bloquea activar/unirse/crear con 503 sin mutaciones, no habilita la venta libre legacy y permite pagar, expirar, cancelar y devolver Compras existentes. El replay idempotente de `crear` sigue funcionando porque se evalúa antes del flag.
16. 6.1 agregó `CorrelationIdFilter` en el gateway: reutiliza `X-Correlation-Id` del cliente, lo genera si falta o está vacío, lo reenvía al backend y lo devuelve en la respuesta incluso en errores 404/503/401, sin alterar el body de cuatro campos.
17. La fase final de documentación agregó `docs/api-fila-virtual.md` y `docs/operacion-fila-virtual.md`, anotaciones `@Operation`/`@ApiResponses`/`@Tag` en los controllers de fila y Compra, y `DocumentacionApiTest`/`DocumentacionApiGatewayTest`, que comprueban rutas, códigos, campos de DTO y prefijos de la matriz de roles contra la implementación.
18. 6.3 agregó `rollbackConFlagDeshabilitadoDrenaComprasYConservaDatosLegacy` en `CompraConcurrenciaIntegrationTest`: con el flag apagado drena la Compra a `VENCIDA`, devuelve el cupo, conserva `fila_evento`/`tanda_fila`, permite pagar una entrada legacy de un evento **sin fila** con un solo `RegistroPago` (`id_compra=null`) y mantiene el 503 para nuevas altas. La entrada legacy se creó en evento 21 porque el pago individual sigue rechazándose con 409 cuando el evento tiene fila persistida.

## Estado por grupo de tareas

- **Verificadas (31/35):** 1.1–1.3, 2.1–2.4, 3.1–3.5, 4.1–4.9, 5.1–5.7, 6.1–6.3. El detalle caso por caso está en la tabla de `tasks.md`; la matriz de roles del gateway quedó en 62 casos, `CompraConcurrenciaIntegrationTest` en 39 y `GatewayErrorHandlingTest` en 14.
- **Persistencia (2.1–2.5):** 2.1–2.4 verificadas con PostgreSQL/Testcontainers: restricciones de membresía/Compra/pago, secuencia inmutable, Pago legacy con una sola referencia y `RegistroPago` por entrada, cierre por cupo/tiempo/resolución, PAUSADA y ticks duplicados. 2.5 sigue pendiente: falta una base con datos legacy reales previos al cambio que compruebe también devoluciones, además de pagos y entradas.
- **Concurrencia (3.1–3.5):** verificadas, incluidas carrera activación-reserva con dos transacciones, fallo Feign sin locks tomados, reintento del scheduler, 12 iteraciones de cierre concurrente con creación de Compra (3.3) y cancelación con entrada `USADA`/reintegro único (3.5).
- **API (4.1–4.9):** verificadas con `FilaVirtualControllerTest`, `CompraControllerTest`, `FilaVirtualServiceTest`, `FilaBloqueoIndividualTest`, `CompraPropiedadTest`, `GatewayRoleMatrixTest` y los tests de documentación. Los controllers exponen `@Operation`/`@ApiResponses`/`@Tag`, `docs/api-fila-virtual.md` es el contrato legible y `docs/README.md` lo enlaza.
- **Compra/pago (5.1–5.7):** verificadas, con reserva atómica de seis entradas y QRs/registros, idempotencia 201/409/422, `FALLA` compensada sin extender el turno y hecho agregado con seis IDs de entrada. `DEMORA` permanece `PENDIENTE` por diseño y sigue sin prueba de conciliación.
- **Trazabilidad/configuración (6.1–6.4):** 6.1–6.3 verificadas. El gateway genera/reutiliza y devuelve `X-Correlation-Id`; el flag `ticketya.fila-virtual.enabled` bloquea altas y permite drenar; el procedimiento de rollback y su prueba PostgreSQL están documentados en `docs/operacion-fila-virtual.md`. 6.4 queda parcial: el productor cumple el contrato, pero falta confirmar la deduplicación con los responsables de Notificaciones y Preferencias antes de conectar los consumidores.
- **Integración (7.1–7.2):** sin evidencia. `docker-compose.yml` solo levanta PostgreSQL y MongoDB, no hay Dockerfiles de microservicios y JMeter no está instalado. Por decisión de alcance no se montó la infraestructura E2E completa; se deja el motivo registrado en `tasks.md`.

## Contrato y coordinación de cancelación: verificado

`msvc-evento` (`EventoServiceImpl` → `EntradaClient.cancelarEntradas`) invocaba `POST /eventos/{idEvento}/cancelacion`, mientras `msvc-entrada` expone `POST /api/admin/entradas/eventos/{idEvento}/cancelacion` (`AdminEntradaController`). `design.md` y la tarea 3.5 citaban además una ruta inexistente `POST /api/entradas/eventos/{idEvento}/cancelacion`. Se unificó el contrato en la ruta administrativa real y `EntradaClientContratoTest` fija la URL y el header `X-User-Id`. La coordinación se verifica por capas: `AdminEventoControllerCancelacionTest` comprueba la ruta HTTP y la delegación del reintento, `EventoServiceImplTest` verifica que el reintento vuelve a invocar `EntradaClient`, y `CancelacionVerificacionIntegrationTest` comprueba los efectos en PostgreSQL. No se levantaron ambos microservicios en el mismo entorno; aun así, los límites observables de 3.5 tienen evidencia verde y la tarea queda marcada.

## Verificación PostgreSQL y HTTP de los cambios recientes

`CancelacionVerificacionIntegrationTest` (Testcontainers `postgres:16`) cubre seis caminos con base real:

1. `FALLA` de pasarela: Compra `RECHAZADA`, Pago `FALLIDO`, entradas `CANCELADA`, cupo de tanda restaurado a 4 y nueva Compra posible conservando el `venceEn` original del turno.
2. Cancelación con reintegro fallido y posterior reintento: entrada en `EN_REINTEGRO` y luego `DEVUELTA`, siempre con `motivoInvalidacion=CANCELACION`, misma clave de reintegro, fila `CERRADA` con causa `CANCELACION`.
3. Dos ingresos con la misma secuencia en un evento son rechazados por la restricción única `uk_entrada_fila_evento_secuencia`.
4. Compra pagada de dos entradas con una `USADA`: cancelación marca ambas como `EN_REINTEGRO`, conserva el total y la clave de reintegro, deja un `RegistroPago` por entrada y el reintento posterior produce un solo Pago `REEMBOLSADO`; repetir el reintento no cambia la clave ni duplica el reintegro.
5. Pago y cancelación concurrentes: la fila queda `CERRADA` por `CANCELACION` y el resultado es consistente; o la Compra queda `CANCELADA` sin Pagos y con entradas canceladas, o queda `DEVUELTA` con un Pago `REEMBOLSADO` y entradas devueltas.
6. `APRUEBA` y `FALLA` concurrentes sobre la misma Compra: exactamente una respuesta gana, queda un solo Pago y no hay emisión parcial ni extensión del turno.

`AdminEntradaControllerCancelacionTest` (MockMvc standalone) confirma que `POST /api/admin/entradas/eventos/{idEvento}/cancelacion` devuelve 202, delega en `CancelacionEventoService` y responde 400 sin `X-User-Id`. `AdminEventoControllerCancelacionTest` hace lo propio para `POST /api/admin/eventos/{id}/cancelacion/reintentar`, con delegación en `EventoService` y 400 sin el header.

Con esto las correcciones 8–11 y el cierre de 3.5/5.3 dejan de apoyarse solo en mocks. Las correcciones 14–18 cerraron concurrente, feature flag, correlación del gateway, documentación y rollback, de modo que esas tareas quedan marcadas.

## Huecos adicionales pendientes

Resueltos con prueba nueva en las rondas anteriores y en esta: secuencia mutable, sobrescritura de `motivoInvalidacion=CANCELACION`, ausencia de transición terminal ante `FALLA`, cierre concurrente con creación de Compra (3.3), bloqueo por flag y drenaje con datos legacy (6.2/6.3) y correlación extremo a extremo desde el gateway (6.1).

Pendientes de confirmar antes de marcar las tareas implicadas:

- **2.5:** migraciones aditivas ejecutadas y concurrencia verificada, pero falta una base con datos legacy reales anteriores al change que compruebe entradas, pagos **y devoluciones** existentes.
- **5.3:** `DEMORA` deja la Compra en `RESERVADA` y el Pago en `PENDIENTE`; es un estado de incertidumbre deliberado, pero sigue sin prueba de su conciliación/reintento.
- **6.4:** falta la verificación de compatibilidad del contrato de deduplicación con los responsables de Notificaciones y Preferencias antes de conectar los consumidores (`msvc-notificaciones` aún no expone `POST /api/notificaciones`).
- **7.1/7.2:** no hay dos instancias de `msvc-entrada` ni informe JMeter; las garantías se cubren con PostgreSQL/Testcontainers y la matriz de roles, no con un despliegue E2E ni con prueba de carga.
- `FilaVirtualIndexMigrationTest` y `FilaEventoRepositoryLockQueryTest` siguen comprobando cadenas SQL, no comportamiento real en PostgreSQL; el comportamiento real sí está cubierto por `CompraConcurrenciaIntegrationTest` y `FilaVirtualLockingIntegrationTest`.

## Conclusión

La suite final queda verde con **347 tests** (0 failures, 0 errors, 6 skipped) y el cambio sigue **en curso**: 31 de 35 tareas con evidencia suficiente. Se cerraron los bloqueos funcionales (cancelación, secuencia, motivo de cancelación, `FALLA`), la concurrencia con PostgreSQL, la documentación de API/contratos/rollback, el feature flag y la correlación desde el gateway. Quedan cuatro tareas sin evidencia completa: 2.5 (base legacy real con devoluciones), 6.4 (coordinación de consumidores) y 7.1/7.2 (dos instancias y prueba de carga JMeter), todas con el motivo registrado en `tasks.md`. No se archiva, commitea ni pushea el change.
