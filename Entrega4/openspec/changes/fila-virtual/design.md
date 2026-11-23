# Diseño: fila virtual y compra agrupada

## Contexto

Ver `proposal.md` y las deltas de `fila-virtual` y `compra-agrupada`. La delta de `fila-virtual` también especifica los nuevos paths del gateway; el requisito genérico existente de `gateway-ruteo` no se modifica. Hoy `msvc-entrada` reserva una `Entrada` por solicitud, `Pago` tiene una referencia obligatoria a una entrada y `RegistroPago` vincula un pago con entradas mediante filas `(idEntrada, idPago)`. El gateway valida JWT/roles y reemplaza `X-User-Id`; `msvc-evento` ya devuelve `idOrganizador` en el detalle del evento. Las bases son independientes, no hay foreign keys entre servicios, y `msvc-entrada` debe soportar varias instancias.

## Objetivos / No objetivos

**Objetivos**

- Mantener una única fuente persistida para el orden de la fila, los cupos y el estado de cada compra.
- Hacer que la asignación de turnos, cupos, asientos y pago sea correcta con varias instancias y reintentos.
- Preservar el flujo individual para eventos sin fila y las entradas individuales para QR, disponibilidad y reportes.

**No objetivos**

- Frontend, usuarios anónimos, Redis/broker, administración de la fila por ADMINISTRADOR, reintegros parciales por monto para una entrada de una Compra con varias entradas o una cola durable que garantice FIFO por instante de llegada a la red.

## Decisiones técnicas propuestas

### Persistencia en `msvc-entrada`

Agregar tablas locales; no se crean relaciones JPA ni foreign keys con las bases de Evento, Usuarios o Ubicación:

- `fila_evento`: una fila por `id_evento` (`UNIQUE`), estado, causa de cierre, `max_personas`, `cupo_entradas`, siguiente secuencia e instante programado para la tanda.
- `entrada_fila`: cada ingreso es una membresía histórica separada con usuario, evento, secuencia inmutable, estado, expiración y referencia a tanda. Las filas que salen de la fila se conservan como historial; una nueva unión inserta una nueva membresía con secuencia mayor. Un índice único parcial permite como máximo una membresía vigente por `(id_evento,id_usuario)` y otra restricción conserva secuencias únicas por evento. Si la tanda cierra por cupo agotado, toda persona habilitada que no compró vuelve a espera con la misma secuencia, haya registrado o no `CUPO_TANDA_INSUFICIENTE`. Si cierra por vencimiento de diez minutos, pierde el lugar quien no compró ni registró ese rechazo.
- `tanda_fila`: número de tanda, inicio/cierre, cupo efectivo, cupo restante y remanente transferible. Es el agregado que serializa asignaciones y evita exceder `maxPersonas` o el cupo de entradas.
- `intento_compra`: asociar a usuario, tanda, instante y resultado/código; guardar el intento rechazado con `CUPO_TANDA_INSUFICIENTE` para distinguir a quien ejerció su turno de quien no inició una compra. Un rechazo por cupo no reserva asientos ni descuenta cupo.
- `compra`: usuario, evento, estado, instante de expiración heredado, total como snapshot, clave de idempotencia y huella del payload. Un índice único parcial sobre `(id_usuario,id_evento)` donde `estado = 'RESERVADA'` permite como máximo una Compra pendiente por cliente/evento, también entre instancias. `Entrada` gana un `id_compra` opcional y conserva precio individual, sector, asiento, estado y QR.

La compra agrupada debe crear sus entradas en la misma transacción; si cualquier asiento o validación falla, se revierten compra, entradas y descuento de cupo. Los precios se consultan a `msvc-evento` y se guardan por entrada, no se confía en precios del cliente.

`maxPersonas` y `cupoEntradas` se configuran por evento al activar. Si se omite `maxPersonas`, se usa `min(20, entradasDisponibles)`; si se omite `cupoEntradas`, se usa `min(maxPersonas * 2, entradasDisponibles)`, sin quedar por debajo de `maxPersonas`. Los valores informados explícitamente deben ser positivos, cumplir `maxPersonas <= cupoEntradas <= maxPersonas * 6` y no superar la disponibilidad real; superar la disponibilidad con un valor explícito responde 400. La ausencia de parámetros nunca genera un valor por encima del inventario. Si no hay entradas disponibles, la activación responde 409. El evento debe estar `PUBLICADO` y no tener entradas reservadas ni vendidas. Una cancelación del evento lleva la fila a `CERRADA` con causa de cierre `CANCELACION`; ese cierre no habilita venta libre. La venta libre posterior a un cierre manual solo existe mientras el evento siga `PUBLICADO`.

### Estados y ausencia de fila

`INACTIVA` es el estado lógico de un evento que todavía no tiene una fila persistida; no se guarda una fila placeholder. Activar crea la fila directamente en `ACTIVA` después de validar el evento y el inventario. Repetir activación de una fila persistida `ACTIVA` es idempotente; un intento de activación sobre una fila persistida `CERRADA` responde 409. Pausar, reanudar, cerrar o consultar el estado administrativo de un evento sin fila responden 404 porque el recurso todavía no existe. Por tanto, en la tabla de transiciones `INACTIVA` representa la ausencia del recurso y no un estado persistido.

Una membresía que completó su pago pasa al estado terminal `COMPRO` y deja de ser vigente; se conserva como historial y nunca se habilita de nuevo. Si la fila está ACTIVA, PAUSADA o AGOTADA, esa persona puede volver a unirse y obtiene una nueva membresía al final con una secuencia nueva. En CERRADA no se aceptan nuevos ingresos.

La regla para las APIs legacy se basa en la existencia de una fila persistida para el evento, no en que su estado sea activo: `POST /api/entradas` y `POST /api/pagos` se rechazan con 409 para cualquier fila persistida (`ACTIVA`, `PAUSADA`, `AGOTADA` o `CERRADA`). Solo la ausencia de fila conserva el flujo legacy.

### Coordinación de cancelación del evento

`msvc-evento` ya es el dueño del estado del evento y coordina la cancelación existente: persiste `CANCELADO`, invoca sincrónicamente `POST /api/admin/entradas/eventos/{idEvento}/cancelacion` y, si Entrada falla, permite reintentar mediante `POST /api/admin/eventos/{id}/cancelacion/reintentar`. Esta ruta se implementa en `msvc-entrada` por `AdminEntradaController` y hoy ejecuta `CancelacionEventoService` dentro de una transacción local.

No se agrega endpoint ni cambio de código a `msvc-evento`. Se extiende la transacción existente de Entrada para que, al recibir la cancelación, marque la fila `CERRADA` con causa `CANCELACION`, quite a quienes esperan, cancele las Compras en estado reservado/no pagado, invalide sus entradas reservadas, libere asientos y devuelva a la tanda el cupo correspondiente.

Las Compras pagadas se reintegran una vez por Compra por el importe total, no una vez por cada Entrada enlazada al pago. Esto aplica aunque una o más entradas estén `USADA`: la cancelación es responsabilidad del organizador, por lo que reintegra el total de la Compra e invalida todas sus entradas. El procesamiento reutiliza la operación de devolución completa de Compra con una clave de idempotencia persistida y estable entre reintentos; invoca la pasarela una sola vez por pago agrupado y no itera por `RegistroPago`. Un fallo deja la devolución pendiente y el endpoint existente de reintento vuelve a usar la misma clave. La cancelación no emite entradas ni inicia la venta libre.

La llamada desde Evento es síncrona y el estado del evento puede quedar `CANCELADO` aunque Entrada no haya completado; el endpoint de reintento existente es el mecanismo de recuperación. La disponibilidad de Entrada debe seguir rechazando nuevas compras para eventos cancelados/cuya fila está cerrada por cancelación.

### Rutas y DTOs propuestos

Los paths son propuestas concretas para revisión; la autorización por rol se aplica en `GatewayRoleMatrix`, y el ruteo nuevo a `msvc-entrada` en `application.yml`:

| Operación | Ruta propuesta | Request/Response principales |
|---|---|---|
| Unirse a fila | `POST /api/filas-virtuales/eventos/{idEvento}/ingresos` | Sin identidad en body; respuesta con estado, posición y expiración |
| Consultar posición propia | `GET /api/filas-virtuales/eventos/{idEvento}/mi-estado` | Estado, posición, secuencia lógica no editable y expiración |
| Activar/configurar | `PUT /api/organizador/filas-virtuales/eventos/{idEvento}/activacion` | `maxPersonas` opcional (default `min(20, disponibles)`), `cupoEntradas` opcional (default `min(maxPersonas * 2, disponibles)`); estado y parámetros efectivos |
| Pausar/reanudar/cerrar | `POST /api/organizador/filas-virtuales/eventos/{idEvento}/{pausa|reanudar|cierre}` | Respuesta con estado vigente |
| Crear compra/reservar | `POST /api/compras` | `idEvento`, lista de 1–6 selecciones `{idSector,fila,asiento}`; `Idempotency-Key`; respuesta con compra, entradas y expiración |
| Pagar Compra | `POST /api/compras/{idCompra}/pago` | Medio de pago y `Idempotency-Key`; respuesta con el estado agregado de la Compra (`RESERVADA`, `PAGADA`, `RECHAZADA`, `VENCIDA`) |
| Consultar Compra | `GET /api/compras/{idCompra}` | Compra y entradas del usuario autenticado |
| Solicitar devolución completa | `POST /api/compras/{idCompra}/devolucion` | Solicita devolver todas las entradas de la Compra bajo las reglas actuales |
| Resolver/reintentar devolución | `PUT /api/admin/entradas/compras/{idCompra}/devolucion`, `POST .../reintentar` | Operación administrativa equivalente al flujo de devolución de Entrada 1 |

Las rutas de administración de fila exigen `ORGANIZADOR`; ingreso, compra, consulta y devolución de Compra exigen `CLIENTE`; resolver o reintentar una devolución de Compra exige `ADMINISTRADOR`. Se usa el prefijo `/api/organizador/filas-virtuales/**`, no `/api/organizador/eventos/**`, porque este último ya enruta a `msvc-evento`. Las rutas nuevas de fila y compra se agregan explícitamente al gateway y a sus pruebas de matriz; la ruta administrativa de devolución queda bajo el prefijo existente `/api/admin/entradas/**`.

Consultar, pagar o solicitar la devolución de una Compra requiere que `Compra.idUsuario` coincida con el `X-User-Id` derivado por el gateway. Si no coincide, se responde 404 para no revelar la existencia de una Compra ajena. La autorización del gateway por rol no sustituye esta comprobación de propiedad en `msvc-entrada`.

Si el cupo restante no alcanza, `POST /api/compras` responde `409` con el código `CUPO_TANDA_INSUFICIENTE` y `cantidadDisponible`, sin crear Compra ni reservar asientos o cupo; sí persiste el intento para conservar el turno y la secuencia hasta el cierre de esa tanda. La persona puede volver a solicitar dentro del turno si se libera cupo, o confirmar una cantidad menor con una solicitud nueva. Una solicitud modificada requiere una clave de idempotencia nueva; la misma clave con payload distinto responde `422`. La Compra admite de una a seis entradas; el máximo no se reduce dinámicamente en función del cupo restante.

La creación de Compra primero consulta `Idempotency-Key`: la misma clave y payload devuelve la Compra existente, incluso si ya venció; la misma clave con payload distinto responde `422`. Después, si existe una Compra `RESERVADA` del mismo cliente y evento con `vence_en <= now()`, se vence y compensa dentro de la misma transacción antes de insertar; si existe una Compra `RESERVADA` vigente, responde `409`. El índice único parcial sobre `(id_usuario,id_evento)` para estado `RESERVADA` protege también frente a carreras entre instancias. Aplica en fila y en venta libre; al terminarse una Compra, incluida una rechazada definitivamente, se puede crear otra dentro del turno vigente, sin extender el vencimiento. En venta libre cada Compra tiene una reserva propia de diez minutos desde su creación.

### Autorización del organizador propietario

1. El gateway verifica JWT y rol ORGANIZADOR; `IdentityHeadersFilter` deriva `X-User-Id` del claim y elimina headers de identidad/rol enviados por el cliente.
2. `msvc-entrada` llama por Feign a `GET /api/eventos/{idEvento}` y compara `X-User-Id` con `idOrganizador`. Se amplía `EventoClient`'s response DTO para mapear el dato que ya expone Evento; no se agrega endpoint ni consulta a `msvc-usuarios`.
3. Evento inexistente se traduce a 404; timeout, indisponibilidad o respuesta incompatible a 503. En todos los errores no se modifica la fila. El rol ADMINISTRADOR no se propaga ni se admite.

La llamada Feign no lleva JWT y mantiene la limitación general de comunicación interservicio sin identidad/roles de usuario.

### Coordinación y concurrencia en PostgreSQL

- **Orden global de locks:** todos los caminos que bloqueen más de un agregado deben adquirir locks en este orden: (1) advisory lock transaccional del evento, (2) advisory de idempotencia de la operación cuando corresponda, (3) fila del evento, (4) tanda, (5) sectores en orden ascendente de `idSector`. Aplica a activación, ingreso/compra, pago o vencimiento cuando afecten la fila, cierre/apertura de tanda, compensación, cancelación y scheduler; si un camino no necesita un nivel, lo omite sin invertir el orden relativo. La adquisición de sectores múltiples siempre es ascendente. No se hacen llamadas Feign mientras estén tomados locks de fila, tanda o sector.
- **Datos remotos para aperturas:** antes de adquirir los locks, precargar desde Evento y Ubicación el estado del evento, sectores, capacidades y cualquier dato remoto necesario para evaluar disponibilidad. Ante timeout, error o respuesta incompatible se falla cerrado: no se crea tanda, no se habilita a nadie ni se cambia estado/cupo; la operación administrativa responde 503 y el scheduler vuelve a intentar. Ya con los locks adquiridos, se revalida la disponibilidad local y los asientos bajo los locks correspondientes. Los parámetros efectivos de activación quedan persistidos en la fila. La disponibilidad que dependa de servicios remotos no se consulta bajo el lock de fila.
- **Elegibilidad de evento:** activar requiere `PUBLICADO`, cero entradas reservadas y cero vendidas. Verificar el estado y la disponibilidad en la operación de activación.
- **Activación frente a reservas individuales:** usar un advisory transaction lock por `idEvento`: exclusivo en activación y compartido al comienzo de la creación de reservas individuales y compras. Los locks compartidos permiten que reservas simultáneas del mismo evento continúen en paralelo; activación espera a que terminen. Después de tomar el lock exclusivo, se comprueban reservas/ventas y disponibilidad. Así la reserva que entra primero fuerza 409 en activación y la activación que entra primero hace que la reserva individual posterior observe una fila activa y se rechace. `UNIQUE(id_evento)` es la defensa adicional ante filas duplicadas.
- **Identidad del lock:** usar la variante transaccional `pg_advisory_xact_lock_shared` / `pg_advisory_xact_lock` con clave estable derivada del evento y namespace fijo. Evitar locks Java, que no coordinan instancias. Mantener el lock hasta commit/rollback.
- **Aperturas de tanda y transiciones:** luego de precargar datos remotos y adquirir los locks en el orden global, bloquear la fila del evento (`SELECT ... FOR UPDATE`) y aplicar transiciones con `UPDATE ... WHERE estado = :esperado`; si la cantidad de filas actualizadas no es uno, releer estado y devolver idempotencia o 409. Un solo proceso puede abrir una tanda o cambiar su estado.
- **Cupo:** descontar tickets mediante actualización condicional atómica del cupo restante (p. ej. `UPDATE tanda_fila SET cupo_restante = cupo_restante - :cantidad WHERE id = :id AND estado = 'ABIERTA' AND cupo_restante >= :cantidad`). Si no actualiza una fila, no hay cupo suficiente. Compra, entradas, cupo y asientos comparten transacción.
- **Orden de asignación de cupo:** cada solicitud real compite por el lock transaccional de la tanda; la asignación sigue el orden en que PostgreSQL concede ese lock. No se garantiza FIFO por el instante en que los paquetes llegaron a la red. Una cola durable estrictamente FIFO queda fuera de alcance.
- **Asientos/capacidad:** conservar el bloqueo actual por evento-sector para no vender doble; una compra con varios sectores toma locks en orden ascendente de `idSector` para reducir deadlocks. Revalidar asientos y capacidad dentro de la transacción antes de insertar todo el grupo.
- **Disponibilidad real:** Evento entrega los sectores configurados; `msvc-entrada` obtiene capacidades de Ubicación por Feign antes de tomar locks y resta entradas activas/reservadas propias bajo la transacción. Excluir reservadas vigentes y vendidas (`VALIDA`/`USADA`); incluir espacios liberados al próximo recálculo. Esta suma limita cupos pero no sustituye las restricciones atómicas de asiento/capacidad.
- **Inicio de reservas:** hacer todos los fetches Feign necesarios antes de adquirir locks de base de datos; luego tomar el lock compartido de evento antes de decidir si corresponde flujo individual o compra agrupada y conservarlo hasta finalizar la transacción.

### Reloj, cierre de tandas y agotamiento

- Al activar, crear la fila en ACTIVA y abrir la primera tanda solo si ya hay personas en `EN_ESPERA`; si no las hay, la fila queda ACTIVA sin tanda en curso. Cuando alguien se une a una fila ACTIVA sin tanda en curso, abrir una tanda inmediatamente. Si ya hay una tanda abierta, quien se una queda en `EN_ESPERA` para la siguiente, aunque la tanda actual no haya alcanzado `maxPersonas`. El turno individual dura diez minutos desde el inicio de la tanda; la Compra hereda ese mismo vencimiento y no inicia otro reloj.
- Cerrar la tanda al ocurrir primero una de estas condiciones: (a) vence el turno de diez minutos; (b) todas las personas habilitadas compraron o salieron de la fila, y no queda ninguna Compra `RESERVADA`; o (c) el cupo restante llega a cero y no queda ninguna Compra `RESERVADA` que pueda devolver cupo. Mientras exista una Compra `RESERVADA`, no cerrar por (b) ni (c). Ante (c), todas las personas habilitadas que no compraron vuelven a `EN_ESPERA` conservando su secuencia original, hayan recibido o no `CUPO_TANDA_INSUFICIENTE`, porque se agotó el cupo antes de que pudieran actuar. Solo ante (a) sale de la fila quien deja vencer el turno sin comprar ni haber registrado ese rechazo. Un pago definitivamente rechazado compensa la Compra, pero no resuelve el turno: el usuario puede iniciar otra Compra dentro del turno vigente.
- Al cerrarse una tanda, abrir inmediatamente la siguiente solo si la fila está ACTIVA, hay al menos una persona en `EN_ESPERA` y hay disponibilidad; el cupo efectivo es `min(cupoEntradas + remanenteAnterior, entradasDisponibles)`. Si no hay disponibilidad y quedan personas esperando, pasar a `AGOTADA`. Si no quedan personas esperando, conservar ACTIVA sin tanda en curso: no abrir una tanda vacía ni reevaluar repetidamente el cierre por resolución completa. PAUSADA detiene aperturas, no los relojes; al reanudar se abre una nueva tanda solo si hay personas esperando y disponibilidad.
- Verificar cierre y apertura bajo el lock de fila del evento y mediante actualización condicional del estado. Revaluar ante Compra pagada, vencida o rechazada. Un `@Scheduled` respalda el vencimiento por tiempo. Los eventos/ticks duplicados no pueden cerrar/abrir dos veces la tanda. El ritmo de venta depende de que las personas resuelvan rápido; el organizador lo dimensiona con `maxPersonas` y `cupoEntradas`.
- El cierre manual elimina a quienes esperan, pero deja correr los turnos vigentes. Después del último turno vigente, las compras grupales pasan a venta libre solo si el evento sigue `PUBLICADO`; cada compra libre tiene una reserva ordinaria de diez minutos desde su creación. Cada cliente puede tener como máximo una Compra `RESERVADA` por evento; cuando termina como pagada, vencida o rechazada, puede crear otra de hasta seis entradas. El cierre por cancelación también deja la fila `CERRADA`, pero no habilita ventas libres. En ambos casos se bloquean siempre las APIs individuales para ese evento.
- **Vencimiento de tanda (a):** al cumplirse los diez minutos desde el inicio, bajo el lock de fila se vencen y compensan en la misma transacción todas las Compras `RESERVADA` de esa tanda que sigan pendientes; después se cierra la tanda. Este orden no depende de que el `@Scheduled` haya corrido antes. El scheduler es respaldo y debe usar la misma transición condicional. Quienes no compraron ni registraron `CUPO_TANDA_INSUFICIENTE` pierden su lugar por vencimiento; quienes tienen ese rechazo conservan su secuencia conforme al invariante de retorno a `EN_ESPERA`.

La API no ofrece salida voluntaria de la fila en este change; un endpoint `DELETE` para salir se deja fuera de alcance. Al completar el pago, la membresía pasa a `COMPRO`, deja de contar como vigente y queda en el historial. La persona puede unirse otra vez si la fila está ACTIVA, PAUSADA o AGOTADA; se crea una membresía nueva con secuencia al final, sin reactivar la anterior.

### Compra, pago e idempotencia

- Mantener `Pago` para ambos contratos: pago individual legacy y pago único de Compra. Propuesta de esquema: `id_entrada` nullable para pagos legacy, `id_compra` nullable para pagos agrupados, con constraint que exige exactamente una referencia. Aplicar `UNIQUE` a la clave de idempotencia en su ámbito (usuario/entrada para legacy; usuario/Compra o Compra para la nueva operación).
- El `POST /api/compras` es idempotente por usuario + `Idempotency-Key`; guardar hash canónico del evento, orden de entradas y selecciones. Primero buscar la clave: igual clave/igual hash devuelve la Compra existente; distinta huella da 422. Luego vencer y compensar en la misma transacción cualquier Compra `RESERVADA` del usuario/evento con `vence_en <= now()` antes de intentar insertar. Una Compra pendiente vigente provoca 409; el índice único parcial es la garantía concurrente. El pago es idempotente por Compra + clave y huella, y la Compra admite como máximo un pago exitoso. Se permite una Compra `RESERVADA` a la vez por usuario/evento, no una Compra total por turno. El pago definitivamente rechazado terminaliza y compensa esa Compra, pero no consume el turno: se puede crear otra durante el mismo vencimiento. Un pago que sigue EN_CURSO puede reintentarse con la misma clave hasta el vencimiento.
- Para un pago agrupado, persistir un `Pago` asociado a `Compra` y crear una fila `RegistroPago` por entrada, usando la clave compuesta existente `(idEntrada,idPago)`. Así se mantiene el historial por entrada sin que los demás servicios conozcan la Compra. El estado del pago único es la autoridad interna para cobrar; las filas del registro no representan cobros independientes. El campo `estado` de la respuesta HTTP informa el estado agregado de la Compra, distinto del estado interno del `Pago` (por ejemplo, `RECHAZADA` frente a `RECHAZADO`).
- Una Compra confirmada emite un único hecho agregado `CompraConfirmada` por Compra hacia `msvc-notificaciones` con sus entradas como colección, y un `EntradaEmitida` por entrada para preferencias. Se reutiliza el mecanismo arquitectónico vigente: llamada Feign fire-and-forget desde `msvc-entrada`, protegida con Resilience4j, sin introducir broker. La llamada no forma parte del resultado transaccional de la compra ni una falla de notificaciones revierte el pago. `msvc-notificaciones` deduplica el aviso por `idCompra`; preferencias conserva la granularidad/idempotencia por `idEntrada`.
- `msvc-entrada` publica ambos tipos mediante Feign fire-and-forget asíncrono, después del commit y protegido por CircuitBreaker Resilience4j, sin broker. El contrato de solicitud es `POST /api/notificaciones` con `tipo`, `idUsuario`, `correlationId` y `datos`; `COMPRA_CONFIRMADA` lleva la lista de IDs de entrada y `ENTRADA_EMITIDA` se envía por entrada. El servicio consumidor de este workspace solo declara esa ruta en su documentación de paquete y todavía no implementa el controller receptor; debe coordinarse con sus responsables antes de esperar entregas exitosas. Las llamadas fallidas no revierten ni demoran la compra.
- Procesar confirmación y vencimiento con actualizaciones condicionales mutuamente excluyentes de Compra: pago solo si `estado = RESERVADA AND vence_en > now()`; vencimiento solo si `estado = RESERVADA AND vence_en <= now()`. Ganando pago se confirman todas las entradas y se emite un QR individual por entrada; ganando vencimiento se cancelan/liberan todas las reservas y el cupo. No usar `@Version` como único control, porque se requiere una condición explícita de plazo y estado en SQL.
- Si llega una solicitud de pago cuando `vence_en <= now()`, no se inicia ni registra un cargo; la Compra se vence y compensa y la operación responde con error. La transición de vencimiento debe ganar condicionalmente antes de cualquier efecto de cobro.
- Ante rechazo/falla definitiva de pago, terminalizar la Compra y aplicar compensación atómica: liberar asientos, cancelar entradas reservadas y restituir cupo. El turno continúa vigente; el cliente puede crear otra Compra sin extender su vencimiento. Un intento que sigue en curso puede reintentarse idempotentemente dentro del mismo plazo, sin crear otro cargo ni extenderlo. Si se reemplaza el mock por un proveedor externo, una aprobación externa tardía después de que vencimiento ganó requiere compensación de pago (reintegro) y no debe emitir entradas; ese proveedor aún no existe en el código actual.
- La devolución completa de Compra valida todas las entradas contra las reglas de Entrega 1, rechaza con 409 si alguna está `USADA`, y efectúa una sola transición condicional del pago/Compra. Requiere una clave de idempotencia propia del reintegro persistida en Compra; el reintento con la misma clave no puede volver a reembolsar. La Compra termina `DEVUELTA` cuando el reintegro se confirma. La cancelación de evento reutiliza esta operación de pago completo una vez por Compra, con la misma clave persistida, y aplica la regla específica de cancelación a entradas que podrían estar `USADA`.
- La pasarela actual **no soporta reintegros por monto**: `PagoService.reintegrarEntrada` opera sobre el pago completo y la configuración `PASARELA_RESULTADO` solo simula aprobación/falla. Por tanto, para una Compra con varias entradas, la devolución de una entrada individual se rechaza 409; no se debe invocar el reintegro total del pago. La devolución individual de una Compra de una sola entrada se trata como devolución completa de esa Compra.
- La respuesta 409 para una devolución individual dentro de una Compra múltiple debe informar claramente que esa operación no está disponible y que el cliente puede devolver la Compra completa. No se crea una solicitud de revisión ni se cambia el pago o las entradas.
- Cada entrada conserva su precio individual en el snapshot de Compra. Un change futuro puede usarlo para instrumentar reintegros parciales por monto con la pasarela y el registro idempotente correspondiente; esa capacidad no forma parte de este change.

### Trazabilidad

Se reutiliza el `correlationId` generado por el gateway cuando falta en la solicitud y propagado por el interceptor Feign existente, con el valor disponible en MDC. Entrada registra con ese identificador el ingreso a fila, Compra, devolución, cancelación y reintentos, junto con los IDs de dominio necesarios para correlacionar el recorrido sin registrar datos sensibles. Los hechos `CompraConfirmada` y `EntradaEmitida` conservan el mismo `correlationId` de la operación que los originó.

## Decisiones registradas en ADR

- ADR-0004: `cupoEntradas` independiente de `maxPersonas`, no derivado como seis por persona.
- ADR-0005: no habilitar de inmediato a quien espera cuando se libera cupo.
- ADR-0006: AGOTADA requiere reanudación manual.
- ADR-0007: cerrar la fila pasa a venta grupal libre tras los turnos vigentes, no detiene ventas.
- ADR-0008: asignar el cupo a solicitudes reales mediante lock transaccional, sin reservarlo anticipadamente ni garantizar FIFO por llegada de red.
- ADR-0009: expirar no reingresa automáticamente a la persona; requiere unirse otra vez.
- ADR-0010: no propagar roles en un header hacia los microservicios; solo el organizador propietario administra.
- ADR-0011: Compra es una entidad explícita; no representar el grupo solo con un identificador en Entrada.
- ADR-0012: una Compra hereda el reloj de diez minutos de la habilitación; no inicia un segundo reloj.
- ADR-0013: mantener el tope fijo de seis entradas por Compra y no derivarlo dinámicamente del cupo de cada tanda.
- ADR-0014: advisory transaction lock por evento para coordinar activación y reservas.
- ADR-0015: rechazar con 409 la devolución individual de una Compra múltiple, sin solicitud de revisión.
- ADR-0016: limitar a una Compra pendiente por cliente y evento, también en venta libre.
- ADR-0017: cerrar cada tanda por vencimiento, resolución completa o cupo agotado; sin intervalo fijo entre tandas.
- ADR-0018: el rechazo definitivo de pago compensa la Compra, pero no consume el turno.


## Riesgos / trade-offs

- **[Cupo aparente frente a disponibilidad]** Un cupo válido puede bajar por ventas/devoluciones entre tandas → recalcular cada apertura y mantener restricciones reales de asiento/capacidad en cada escritura.
- **[Bloqueo de Evento durante Feign]** Dependencia remota puede alargar la transacción → precargar datos remotos y mantener bloqueos solo para la sección crítica; fallar cerrado si la propiedad/disponibilidad no se puede validar.
- **[Deadlocks multiasiento]** Compras con varios sectores pueden tomar locks en distinto orden → ordenar sector IDs y probar cargas concurrentes.
- **[Pago externo tardío]** Una pasarela real puede aprobar después del vencimiento → el estado condicional impide emitir; el futuro adaptador deberá reintegrar con idempotencia.
- **[Proveedor de pago externo]** La pasarela actual es una simulación local dentro de la transacción y no realiza cobros externos. Si se integra un proveedor real, el cobro debe ejecutarse fuera de la transacción y sin locks de fila/tanda/sector; después se confirma localmente con una actualización condicional. Si el proveedor aprueba pero la Compra ya venció o la actualización condicional no gana, no se emiten entradas y se debe reintegrar el cobro con una clave idempotente.
- **[Receptor de notificaciones ausente]** `msvc-notificaciones` todavía no implementa `POST /api/notificaciones` en este workspace → el cliente queda integrado al contrato documentado, pero las entregas reales requieren que el consumidor publique el endpoint.
- **[Pasarela sin reintegro por monto]** La implementación mock solo representa reintegros completos → rechazar la devolución individual de una entrada en una Compra múltiple con 409; permitir solo la devolución completa del grupo.
- **[Diferencia entre Compra y entradas para avisos]** Una Compra genera una sola notificación al comprador, pero hasta seis registros individuales de preferencias → evento `CompraConfirmada` con un agregado de entradas para notificaciones y procesamiento idempotente por `idEntrada` en preferencias.
- **[Venta libre cerrada sin turno]** No hereda un reloj de turno → cada Compra libre recibe diez minutos desde su creación, sin alterar los vencimientos de turnos anteriores.
- **[Ingreso durante una tanda abierta]** Una persona que se une cuando ya hay una tanda en curso espera a la siguiente, aunque queden lugares por debajo de `maxPersonas`; se prioriza evitar modificar la composición y el vencimiento de una tanda ya iniciada.

## Migración / despliegue

1. Agregar tablas y columnas nullable/restricciones compatibles con filas existentes; los datos históricos permanecen como compras/pagos individuales sin `idCompra`.
   En la implementación actual, Hibernate `ddl-auto: update` crea/actualiza las tablas al inicializar el `EntityManagerFactory`; `FilaVirtualIndexMigration` se ejecuta después como `ApplicationRunner` y crea los índices parciales y la restricción de referencia de Pago. La migración toma un advisory transaction lock de namespace propio antes del DDL, para serializar los arranques concurrentes; sus `IF NOT EXISTS` permiten reintentos seguros.
   La misma migración agrega `uk_compra_reservada_usuario_evento` sobre `(id_usuario,id_evento)` con predicado `estado = 'RESERVADA'`.
2. Desplegar Entrada con endpoints y lógica nueva; desplegar la matriz/ruteo de Gateway coordinadamente.
3. No activar filas hasta que ambos componentes estén desplegados y las rutas hayan pasado las pruebas de autorización y ruteo.
4. `ticketya.fila-virtual.enabled=false` impide nuevas activaciones, ingresos y creación de Compras; permite completar o expirar Compras ya existentes y procesar cancelaciones/devoluciones. Los endpoints legacy siguen consultando la fila persistida y nunca habilitan una venta individual para eludirla.
5. Rollback: deshabilitar la función, drenar o resolver turnos y Compras pendientes, conservar tablas/columnas y datos ya escritos y no revertir el servicio a una versión que desconozca filas/Compras activas. No migrar ventas existentes a grupos.

## Decisiones cerradas

- `maxPersonas` es configurable por evento al activar, con default `min(20, disponibles)`; `cupoEntradas` omitido usa `min(maxPersonas * 2, disponibles)` y no queda por debajo de `maxPersonas`.
- La venta libre manual post-cierre requiere que el evento siga `PUBLICADO`; cada Compra tiene diez minutos desde su creación.
- La cancelación marca la fila `CERRADA` con causa de cierre y no habilita venta libre.
- El pago rechazado terminaliza y compensa la Compra, pero no consume el turno; mientras el turno siga vigente se puede crear otra Compra, sin extender el vencimiento.
- El orden transaccional por lock de tanda es suficiente; FIFO estricto por tiempo de llegada de red y una cola durable quedan fuera de alcance.
- Solo se activa la fila para un evento `PUBLICADO` sin entradas reservadas ni vendidas.
- `INACTIVA` representa que no hay una fila persistida; operaciones de administración sobre un evento sin fila responden 404.
- Cada ingreso se conserva como una membresía histórica; reingresar tras salir crea una nueva fila con nueva secuencia.
- Solo puede existir una Compra `RESERVADA` por persona y evento a la vez; un rechazo definitivo la compensa, pero no consume el turno ni impide crear otra Compra mientras el turno siga vigente.
- Los locks se adquieren en orden global: advisory del evento, fila del evento, tanda y sectores en orden ascendente de ID; no se ejecutan llamadas Feign con locks de fila, tanda o sector tomados.
- Una membresía pagada pasa al estado terminal `COMPRO` y deja de ser vigente; puede reingresar en una fila abierta a ingresos mediante una nueva membresía y secuencia.
- Los errores o timeouts de Evento/Ubicación al abrir una tanda fallan cerrados sin crearla ni cambiar estado/cupo; el scheduler reintenta.
- Solo se abre una tanda cuando hay al menos una persona en `EN_ESPERA`. La fila activa puede permanecer sin tanda; un ingreso la abre si no hay otra en curso, y si ya hay tanda abierta espera a la siguiente.
