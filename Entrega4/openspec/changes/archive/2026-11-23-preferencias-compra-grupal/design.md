# Design

## Context

Ver `proposal.md` para la motivación. `msvc-entrada` ya posee el outbox `pendientes_preferencia` (tabla propia, con `uk_pendiente_preferencia_id_entrada`), el worker `PreferenciasOutboxWorker` con reclamo atómico y backoff, y el cliente Feign hacia `msvc-preferencias`. El flujo CRUD legado (`PagoService.crear`) ya inserta el pendiente; el flujo de compra agrupada (`CompraService.pagar`) no lo hace. La transacción de `pagar` ya cubre pago, entradas, QR y `CompraPagadaEvent`. No hay spec viva de `entrega-compras-preferencias`, por eso el delta es `ADDED` y no `MODIFIED`.

## Goals / Non-Goals

**Goals:**
- Cerrar la brecha entre los dos flujos de pago sin cambiar el contrato HTTP ni el esquema de datos.
- Mantener la atomicidad: si la transacción de pago se revierte, no queda ningún aviso.
- No duplicar avisos ante reintentos de pago con la misma clave de idempotencia.
- No romper los tests unitarios que instancian `CompraService` directamente.

**Non-Goals:**
- No cambiar el worker, el backoff, el circuit breaker ni el contrato `/internal/preferencias/compras`.
- No generar preferencias para participantes invitados (sin identidad de usuario garantizada).
- No tocar notificaciones, devoluciones, JMeter ni la colección Postman existente.

## Decisions

**1. Reutilizar el outbox existente en lugar de publicar un evento nuevo.**
El aviso ya se persiste y se procesa; agregar un mensaje implicaría otro patrón de entrega y una tabla/consumidor nuevos. Alternativa descartada: llamar sincrónicamente a `msvc-preferencias` desde `pagar` — rompería la regla de no esperar a preferencias y eliminaría la tolerancia a fallas del worker.

**2. Insertar los pendientes dentro de la transacción de `pagar`, justo antes de publicar `CompraPagadaEvent`.**
Así la restricción de unicidad y el `@Transactional` existentes garantizan atomicidad y funcionan con las dos instancias de `msvc-entrada`. Alternativa descartada: un `@TransactionalEventListener(AFTER_COMMIT)` como el de notificaciones — permitiría que el pago se confirmara sin el aviso si el listener falla, justamente el problema que se corrige.

**3. Titular = `Compra.idUsuario`, uno por entrada.**
Cada entrada tiene su propio `idEntrada` (clave natural del outbox) y el usuario pagador es el único garantizado por el modelo de compra. Alternativa descartada: usar `Entrada.idUsuario` — en la compra agrupada las entradas pueden corresponder a invitados; eso crearía preferencias para identidades no verificadas.

**4. Verificación previa por `findByIdEntrada` + dependencia opcional del repositorio.**
`PagoService` ya hace exactamente eso. Mantener el mismo helper evita divergencia. La inyección opcional por setter (`@Autowired(required = false)`) preserva los 7 tests que llaman al constructor sin ese argumento y cualquier perfil sin la tabla. La unicidad en BD sigue siendo la última línea de defensa.

**5. Sin ADR nuevo.**
No se introduce tecnología ni patrón arquitectónico: se reutiliza el outbox persistido ya decidido en `recomendaciones-preferencias` (ADR de la Entrega 2). No hay nada nuevo que documentar en `docs/decisiones/`.

## Risks / Trade-offs

- **[Riesgo] El worker procesa las filas antes de que se confirme la transacción** → Mitigación: los inserts ocurren dentro de `@Transactional`; el reclamo atómico solo ve filas ya commiteadas.
- **[Riesgo] Un pago repetido con distinta clave intente duplicar avisos** → Mitigación: `findByIdEntrada` más la restricción única `uk_pendiente_preferencia_id_entrada`.
- **[Riesgo] Instancias concurrentes inserten la misma entrada** → Mitigación: la misma restricción única; ambas instancias comparten la BD.
- **[Trade-off] Una sola preferencia para varios participantes** → Se acepta: el invitado no tiene identidad de usuario contractual y el alcance pedido es simplificar al comprador.
- **[Riesgo] `GET /api/preferencias/clientes/{id}` aún pueda dar 404 si el worker no ha corrido** → Mitigación: el estado final es asíncrono por diseño; el E2E debe esperar el procesamiento del outbox antes de asertar 200.
