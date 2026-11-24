# Proposal: aviso de devolución aprobada

## Por qué
La pantalla promete "te avisaremos cuando se acredite", pero nadie envía `DEVOLUCION_APROBADA`: el tipo y la plantilla existen en msvc-notificaciones (`evento`, `idEntrada`, `monto`) y no hay productor.

## Qué cambia
- msvc-entrada: nuevo `DevolucionAprobadaEvent`. Se publica cuando el reintegro de una devolución queda concretado (la Compra pasa a DEVUELTA, tanto al aprobar como al reintentar un reintegro pendiente, o la entrada pasa a DEVUELTA en el flujo individual).
- Un listener asíncrono (después del commit) arma el pedido y lo envía por `NotificacionesClient`, que lo traduce al contrato: evento, ids de entrada y monto.
- Clave de idempotencia: `DEVOLUCION_APROBADA:idCompra:<id>` (o `idEntrada` en el flujo individual).

## Fuera de alcance
- Cancelación de evento: ya avisa `EVENTO_CANCELADO`; no se envía además un aviso de devolución.
- No hay aviso por devolución rechazada.

## Riesgos
- Si el reintegro queda pendiente (`FALLA` en la pasarela) no se avisa hasta que se complete.
- Un fallo de msvc-notificaciones no afecta la devolución: queda en el log.
