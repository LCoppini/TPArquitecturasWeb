# Catalogo de eventos de dominio (EDA)

> Propuesta inicial: ajustar en grupo. Cada servicio define sus propios eventos (sin libreria compartida).

| Evento | Publica | Consume | Para que |
|---|---|---|---|
| EventoPublicado | msvc-evento | (E2) notificaciones | Avisar a clientes cuyo artista favorito publica en su ciudad |
| EventoCancelado | msvc-evento | msvc-entrada | Pasar entradas vendidas a reintegro |
| PagoAprobado / PagoRechazado | msvc-entrada | msvc-entrada | Emitir entrada o liberar lugares |
| ReservaVencida | msvc-entrada | msvc-entrada | Liberar lugares |
| CompraConfirmada | msvc-entrada | (E2) notificaciones | Enviar un único aviso por Compra; contiene sus entradas |
| EntradaEmitida | msvc-entrada | (E2) preferencias | Registrar cada entrada de forma idempotente en el historial del cliente |
| DevolucionAprobada | msvc-entrada | (E2) notificaciones | Avisar reintegro |

## Contrato de la fila virtual

- `CompraConfirmada`: un único hecho por Compra. La clave de deduplicación del consumidor es `idCompra`; el payload incluye los `idEntrada` de la Compra.
- `EntradaEmitida`: un hecho por entrada. La clave de deduplicación del consumidor es `idEntrada`.
- Ambos eventos se emiten después del commit de la compra aprobada, con el mismo `correlationId` del request.
- `msvc-notificaciones` debe deduplicar `CompraConfirmada` por `idCompra` y `msvc-preferencias` debe deduplicar `EntradaEmitida` por `idEntrada` antes de adoptar el contrato.
- El productor ya cumple este contrato: `CompraEventoPublicacionTest` verifica un único `CompraPagadaEvent` por Compra con `idCompra`, los seis `idEntrada` y el `correlationId` del request; `CompraConcurrenciaIntegrationTest` verifica un QR y un `RegistroPago` por entrada. La adopción por los consumidores sigue pendiente de coordinación con sus responsables y no se implementa en este change. `msvc-notificaciones` todavía no expone `POST /api/notificaciones`.
