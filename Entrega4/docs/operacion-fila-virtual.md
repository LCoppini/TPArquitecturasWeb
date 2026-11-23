# Operación y rollback de la fila virtual

El cambio es aditivo: las tablas `fila_evento`, `tanda_fila`, `entrada_fila`,
`intento_compra` y `compras` se crean sin tocar las columnas de las entradas
históricas de Entrega 1. Los índices parciales y la restricción de pago con una
sola referencia se aplican con `CREATE ... IF NOT EXISTS` en
`FilaVirtualIndexMigration`, por lo que relanzar la migración es seguro.

## Desactivar el uso de fila

`ticketya.fila-virtual.enabled` (variable `FILA_VIRTUAL_ENABLED`, default `true`)
está en `msvc-entrada/src/main/resources/application.yml` y en la configuración
centralizada `msvc-config/.../msvc-entrada.yml`. Con `false`:

- `PUT /api/organizador/filas-virtuales/.../activacion`, el ingreso a la fila y
  `POST /api/compras` responden 503 sin tomar locks ni mutar datos.
- No se habilita el fallback de venta libre para eventos con fila persistida:
  la ruta de Compra sigue cerrada con 503.
- Siguen disponibles el pago, la devolución, la cancelación y la expiración de
  Compras ya creadas, y el lector de entradas legacy.

## Procedimiento de drenaje

1. Desactivar el flag y esperar a que no se inicien nuevas Compras (503 en
   `POST /api/compras`).
2. Dejar expirar los turnos: el scheduler `FilaVirtualScheduler` y
   `expirarComprasVencidas` compensan las `RESERVADA` vencidas, devuelven cupo y
   cancelan sus entradas sin QR.
3. Cerrar o pagar las Compras pendientes restantes con el flujo normal. Para una
   cancelación administrativa, usar
   `POST /api/admin/entradas/eventos/{idEvento}/cancelacion` y su reintento
   `POST /api/admin/eventos/{id}/cancelacion/reintentar`.
4. Verificar que `hayComprasEnCurso(idEvento)` sea `false` antes de cualquier
   cambio de binario.
5. Conservar las tablas y los datos: no ejecutar `DROP` ni borrar `compras`,
   `pagos` o `entradas`. La configuración puede revertirse a `true` sin migrar
   datos.

## Restricción de binarios

No desplegar una versión anterior al cambio mientras existan filas o Compras
activas: esos binarios desconocen `Compra`, `TandaFila` y la columna `id_compra`
de `pagos`. Solo después de drenar y confirmar que no quedan Compras `RESERVADA`
ni `PAGADA` pendientes de reintegro se puede evaluar una reversión, y aun así se
recomienda mantener el esquema.

## Compatibilidad con datos históricos de Entrega 1

Las entradas creadas antes del cambio tienen `id_compra = NULL` y siguen
pagándose por `POST /api/pagos` con un único `RegistroPago` por entrada, siempre
que el evento no tenga una fila persistida (los eventos con fila usan
`POST /api/compras`). El pago
introduce la restricción de una sola referencia: `id_entrada` **o** `id_compra`,
nunca ambas. `CompraConcurrenciaIntegrationTest` verifica contra PostgreSQL que
una fila legacy se persiste y paga con su `RegistroPago`, y que la restricción
rechaza un pago con las dos referencias. Ninguna migración reescribe esas filas.
