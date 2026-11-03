# Catalogo de eventos de dominio (EDA)

> Propuesta inicial: ajustar en grupo. Cada servicio define sus propios eventos (sin libreria compartida).

| Evento | Publica | Consume | Para que |
|---|---|---|---|
| EventoPublicado | msvc-evento | (E2) notificaciones | Avisar a clientes cuyo artista favorito publica en su ciudad |
| EventoCancelado | msvc-evento | msvc-entrada | Pasar entradas vendidas a reintegro |
| PagoAprobado / PagoRechazado | msvc-entrada | msvc-entrada | Emitir entrada o liberar lugares |
| ReservaVencida | msvc-entrada | msvc-entrada | Liberar lugares |
| EntradaEmitida | msvc-entrada | (E2) notificaciones | Confirmar compra |
| DevolucionAprobada | msvc-entrada | (E2) notificaciones | Avisar reintegro |
