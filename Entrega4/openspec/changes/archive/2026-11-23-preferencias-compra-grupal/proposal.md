# Proposal: preferencias en la compra agrupada

## Why

El flujo `POST /api/compras/{idCompra}/pago` de `CompraService.pagar(...)` aprueba la compra, genera los QR y publica `CompraPagadaEvent`, pero **no inserta el aviso en `pendientes_preferencia`**. Ese inserto existe solo en el flujo CRUD legado (`PagoService.crear`). Como el worker de preferencias lee únicamente esa tabla, el cliente que compra agrupando entradas nunca obtiene su documento de preferencias: `GET /api/preferencias/clientes/{idUsuario}` responde 404. La colección E2E de Postman tuvo que relajar H1/H4 para aceptar 200 o 404; con este cambio el camino normal vuelve a ser 200.

## What Changes

- `msvc-entrada`: `CompraService.pagar(...)` registra un `PendientePreferencia` **por cada entrada pagada**, en la misma transacción del pago, usando `compra.getIdUsuario()` como titular.
- El registro es idempotente por `idEntrada` (misma restricción y Verificación previa que `PagoService.registrarPendientePreferencia`), conserva el `correlationId` del pago y no agrega reintentos propios: el worker existente los administra.
- Se reutiliza el repositorio y la entidad existentes; no se crea tabla, endpoint ni mensaje nuevo.
- No se modifica ningún test existente: se agrega uno nuevo que falla con el código actual y pasa con el fix.
- La colección Postman conserva sus aserciones actuales (200 o 404); el fix hace que el camino recorrido sea 200, sin relajar ni reforzar aserciones.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `entrega-compras-preferencias`: agrega el requisito **"Persistencia del pendiente en la compra agrupada [Entrega 2]"**. La capability ya tiene en el cambio archivado `recomendaciones-preferencias` el requisito "Persistencia atómica del pendiente", pero **no hay spec viva** de esa capability, por lo que no es posible un delta `MODIFIED` sin inventar texto base. El delta es `ADDED` y se limita al flujo `POST /api/compras/{idCompra}/pago`, sin reescribir ni borrar el requisito archivado.

## Impact

- Código: `Entrega4/msvc-entrada/src/main/java/com/ticketya/entrada/service/CompraService.java` (nueva dependencia opcional hacia `PendientePreferenciaRepository`, helper privado y una llamada dentro de `pagar`).
- Tests: nuevo test unitario en `msvc-entrada` (`CompraPagoPreferenciaTest`), sin modificar los 7 tests que construyen `CompraService` a mano.
- APIs: sin cambios de contrato. `GET /api/preferencias/clientes/{id}` y `GET /api/admin/preferencias/clientes/{id}` pasan a devolver 200 tras una compra agrupada.
- Datos: más filas en la tabla ya existente `pendientes_preferencia` de la BD de `msvc-entrada`; el worker de `msvc-preferencias` las procesa con el mismo backoff y circuit breaker.
- Configuración: sin cambios; el repositorio se inyecta de forma opcional para no romper los perfiles/tests que no lo proveen.
- Fuera de alcance: notificaciones (el `DESCARTADA` por falta de SMTP está documentado en el change `notificaciones-contrato-entrada`), preferencias de los participantes invitados a la compra, devoluciones y el test de carga JMeter.
