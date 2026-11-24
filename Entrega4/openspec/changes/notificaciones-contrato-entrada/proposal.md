# Proposal: alinear entrada con el contrato de notificaciones

## Por qué
msvc-entrada llamaba a `/api/notificaciones` con `{tipo, datos}`, pero msvc-notificaciones expone `POST /internal/notificaciones` con `{claveIdempotencia, idUsuario, tipo, destinatario?, datos<String,String>}`, rechaza `ENTRADA_EMITIDA` y exige `evento`, `fechaHora` e `idEntrada` en la plantilla de compra confirmada. Los avisos fallaban en silencio. Además msvc-usuarios no devolvía el email y nadie avisaba la cancelación de un evento.

## Qué cambia
- msvc-entrada: `NotificacionesClient` pasa a ser un adaptador (conserva `enviar(NotificacionRequest)`) sobre un nuevo Feign `NotificacionesFeign` hacia `/internal/notificaciones`. Resuelve nombre y fecha del evento con `EventoClient`, genera la clave de idempotencia y descarta `ENTRADA_EMITIDA` (un correo por compra, no por entrada).
- msvc-entrada: al cancelar un evento se publica `EventoCanceladoEvent` con los usuarios con entradas VALIDA y un listener asíncrono (después del commit) envía `EVENTO_CANCELADO`. Un fallo de envío no afecta la cancelación.
- msvc-usuarios ya expone `email` (change perfil-propio-usuario), por lo que notificaciones resuelve el destinatario.

## Fuera de alcance
- `EVENTO_MODIFICADO`: msvc-evento solo permite editar eventos en BORRADOR, que no tienen compradores.
- `DEVOLUCION_APROBADA`.

## Riesgos
- El correo de compra lista los ids de entrada en un solo campo de texto.
