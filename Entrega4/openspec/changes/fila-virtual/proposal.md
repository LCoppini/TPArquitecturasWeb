# Propuesta: fila virtual y compra agrupada

## Por qué

Para eventos de alta demanda, TicketYa necesita dosificar el acceso a la compra por tandas sin perder orden ni consistencia cuando `msvc-entrada` se ejecuta en varias instancias. La compra actual reserva y paga una entrada por operación; una tanda necesita permitir una compra de hasta seis entradas con un único pago y vencimiento coordinado con el turno.

## Qué cambia

- Incorporar en `msvc-entrada` una fila persistida por evento, activada manualmente por su organizador, con hasta `maxPersonas` habilitadas por tanda y un `cupoEntradas` configurable e independiente.
- Agregar ingreso idempotente, consulta de estado/posición y operaciones de activar, pausar, reanudar y cerrar, con autorización por rol en el gateway y validación de propiedad del evento en el servicio.
- Habilitar la primera tanda al activar y abrir las siguientes al cierre de la tanda previa. Cada tanda se cierra por vencimiento de diez minutos, resolución de todos sus habilitados sin Compras pendientes o agotamiento del cupo sin Compras pendientes. La apertura se coordina por lock de fila; no hay intervalo fijo entre tandas.
- Incorporar `Compra` en `msvc-entrada` para agrupar de una a seis entradas del mismo evento, reservarlas atómicamente y pagar su total una sola vez. Cada entrada conserva su precio, asiento, estado y QR.
- En venta libre posterior al cierre manual, permitir una sola Compra `RESERVADA` por cliente/evento; una vez pagada, vencida o rechazada, el cliente puede crear otra Compra de hasta seis entradas.
- Exigir que el evento esté `PUBLICADO` al activar y al permitir venta libre luego de un cierre manual.
- Coordinar la activación de la fila con la creación de reservas individuales en PostgreSQL. Las APIs existentes `POST /api/entradas` y `POST /api/pagos` se conservan para eventos sin fila; se rechazan para eventos con fila, incluso cuando esta está cerrada.
- Documentar las alternativas descartadas en ADRs separados.

## Fuera de alcance

- Interfaz web, detección de desconexión/cierre de página y reingreso automático: quien vence debe volver a unirse y recibe una secuencia nueva.
- Administración de la fila por `ADMINISTRADOR`; en este change solo administra el organizador propietario.
- Salida voluntaria de la fila mediante endpoint `DELETE`; las personas salen por las reglas de vencimiento/cierre descritas, sin una operación de baja explícita.
- Reintegro parcial de una entrada perteneciente a una Compra con varias entradas: la pasarela mock actual no admite importes parciales, por lo que esa devolución responde 409 y queda documentada como limitación/fuera de alcance. Se admite el reintegro completo de una Compra (y la devolución normal de una Compra de una sola entrada), sujeto a las reglas de devolución vigentes.
- Redis, broker, cola durable con FIFO estricto por llegada de red, coordinación por memoria local, reserva fuera de `msvc-entrada`, cambios funcionales en `msvc-notificaciones`/`msvc-preferencias` y reintegro parcial por entrada en una Compra múltiple.

## Capacidades

### Nuevas capacidades

- `fila-virtual`: orden de espera, tandas, turnos, cupos, administración y autorización de compra.
- `compra-agrupada`: reserva atómica de hasta seis entradas y pago único, integrada con el turno.

### Capacidades modificadas

| Capacidad de Entrega 1 afectada | Delta en este change | Motivo |
|---|---|---|
| Entradas y reservas | Se conservan `POST /api/entradas` y las entradas individuales para eventos sin fila. Para eventos con fila, la reserva individual queda bloqueada y se usa una Compra grupal atómica de hasta seis entradas. La cancelación invalida las Compras pendientes; las pagadas se reintegran una vez por el total de la Compra, incluso si alguna entrada está `USADA`, y libera sus reservas/cupo pendientes. | Evitar que el flujo legacy eluda la fila y mantener una única fuente de consistencia para reservas grupales y cancelaciones. |
| Pagos | Se conserva el pago individual legacy para eventos sin fila. Para eventos con fila, se bloquea `POST /api/pagos` y una Compra se confirma con un único pago por el total. Un pago rechazado termina y compensa esa Compra, pero no consume el turno: el cliente puede crear otra Compra dentro del vencimiento vigente. | Mantener compatibilidad con Entrega 1 y permitir reintentar la compra sin extender el turno. |
| Devoluciones | Se conservan las devoluciones legacy. Se agrega devolución completa de Compra y su resolución/reintento administrativo; devolver una sola entrada de una Compra múltiple responde 409 con un mensaje que indica que puede devolverse la Compra completa. Cada entrada conserva su precio para una devolución individual futura. | La pasarela actual no admite reintegros parciales; no se ofrece una solicitud sin posibilidad de resolución ni se reintegra accidentalmente el pago completo por una sola entrada. |
| Ruteo del gateway | Se agregan las rutas de Compras y administración de fila/devolución de Compra a `msvc-entrada`, bajo los prefijos existentes y sin colisionar con los de `msvc-evento`. | Exponer los contratos nuevos conservando la matriz explícita de roles y el ruteo de Entrega 1. |

### Relación con capacidades archivadas de Entrega 1

No se cambia el comportamiento de las capacidades archivadas para eventos sin fila. `POST /api/entradas` y `POST /api/pagos` conservan su flujo individual en esos eventos. La restricción de esas operaciones para eventos con fila es una regla nueva de Entrega 2; para esos eventos se usa `Compra`. Las entradas emitidas siguen siendo unidades individuales para QR, disponibilidad y reportes. Se conservan las reglas de devolución de Entrega 1 y se agrega la devolución completa de Compra; la devolución parcial monetaria de una entrada dentro de una Compra múltiple queda fuera de alcance mientras la pasarela no admita importes parciales.

El requisito genérico existente de `gateway-ruteo` y sus destinos actuales se conservan; los paths nuevos se especifican en `fila-virtual` y se agregan como delta de Entrega 2. Las capabilities de dominio de entradas/reservas, pagos y devoluciones de Entrega 1 no tienen specs base separadas en el árbol OpenSpec actual; sus deltas respecto de los endpoints y comportamientos legacy quedan explicitados en la tabla anterior y en `compra-agrupada`.

## Impacto

- `msvc-entrada`: persistencia de fila, tandas, Compras e intentos; endpoints, cancelación, pagos, devoluciones, vencimientos, integración Feign con Evento y llamada Feign fire-and-forget a Notificaciones protegida por Resilience4j, trazabilidad por `correlationId` y pruebas concurrentes. No se incorpora broker.
- `msvc-gateway`: nuevas rutas; la matriz exige `ORGANIZADOR` para administrar fila, `CLIENTE` para ingreso/compra/devolución y `ADMINISTRADOR` para resolver o reintentar devoluciones. El gateway deriva `X-User-Id` del JWT y no propaga roles.
- `msvc-evento`: requiere ajustar `EntradaClient` a la ruta administrativa real de Entrada; no necesita endpoints nuevos. Ya persiste el evento como `CANCELADO`, llama sincrónicamente a `POST /api/admin/entradas/eventos/{idEvento}/cancelacion` y ofrece `POST /api/admin/eventos/{id}/cancelacion/reintentar` si falla la coordinación; Entrada extiende el procesamiento del contrato existente.
- `msvc-notificaciones`: fuera de alcance implementar cambios en el consumidor; este change define el contrato de `CompraConfirmada` como un único hecho agregado por Compra y requiere coordinar su adopción con quien mantenga ese servicio.
- `msvc-preferencias`: fuera de alcance implementar cambios en el consumidor; se conserva como contrato que `EntradaEmitida` se procese idempotentemente por `idEntrada`, y se coordina con quien mantenga Preferencias.
- `msvc-usuarios` y `msvc-ubicacion`: sin cambios funcionales.
- `msvc-entrada` y `docs/eventos.md`: emitir/documentar `CompraConfirmada` una vez por Compra para notificaciones y mantener `EntradaEmitida` por entrada para preferencias; los cambios funcionales a los consumidores quedan fuera de alcance y requieren coordinación con sus responsables.
- ADRs: `0004` a `0018` en `docs/decisiones/`, para las decisiones y alternativas descartadas del change.
