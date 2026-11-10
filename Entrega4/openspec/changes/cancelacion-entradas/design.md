# Diseño: cancelación de entradas

## Contexto

`msvc-entrada` persiste las entradas y pagos en su propia base. El endpoint de
cancelación delega el proceso completo a `CancelacionEventoService`, que
coordina el cambio de estado de las entradas y el procesamiento de reintegros.
No se agregan dependencias entre bases ni se cambia el propietario del estado
del evento.

## Flujo propuesto

1. La operación de cancelación vence reservas expiradas.
2. El repositorio cambia las entradas todavía `RESERVADA` del evento a
   `CANCELADA` y limpia su expiración. El pago existente solo acepta el estado
   `RESERVADA`, por lo que las reservas canceladas no se pueden pagar.
3. Las entradas `VALIDA` y `USADA` pasan a `EN_REINTEGRO`. La validación del QR
   solo acepta `VALIDA`, por lo que estas entradas dejan de permitir acceso.
4. `PagoService` busca las entradas `EN_REINTEGRO` y sus pagos. Solo procesa
   pagos `APROBADO` o `REINTEGRO_PENDIENTE`; un pago rechazado o meramente
   pendiente no debe generar un reintegro.
5. Si el mock falla, el pago queda `REINTEGRO_PENDIENTE` y la entrada permanece
   `EN_REINTEGRO`. Si el mock responde correctamente, el pago queda
   `REEMBOLSADO`, se guarda una referencia y la entrada pasa a `DEVUELTA`.
6. Una nueva ejecución ignora entradas `DEVUELTA`; por tanto, el mock no vuelve
   a emitir un reintegro para ese procesamiento ya completado.
7. El mock solo acepta `APRUEBA` y `FALLA` como resultados de reintegro. Otro
   valor produce un error y no se considera un reintegro exitoso.

La coordinación de los cambios locales se ejecuta en una única transacción.
Si falla el procesamiento de pagos, también se revierten los cambios de
estado de las entradas.

## Decisiones

- Se usa el estado existente `CANCELADA` para las reservas no pagadas y se
  elimina `expiraEn`, evitando agregar una entidad o columna.
- Las entradas emitidas conservan un estado intermedio `EN_REINTEGRO` para no
  considerarlas devueltas antes de que el reintegro mock termine.
- La operación es repetible para los estados modelados. Un proveedor real debe
  soportar idempotencia propia en la API de reintegros: la garantía del mock no
  debe interpretarse como garantía ante fallos de red con un proveedor externo.
- No se incorpora mensajería ni envío de notificaciones aquí, dado que EP-14
  asigna esa capacidad a Entrega 2.

## Validación

- Prueba de integración PostgreSQL: reserva, entrada válida y entrada usada
  cambian al estado esperado y ninguna sigue ocupando disponibilidad.
- Pruebas unitarias: no permitir pagar una entrada cancelada; dejar el
  reintegro pendiente ante fallo; completar el reintento; ignorar pagos sin
  estado cobrable.
- Ejecutar `mvn -pl msvc-entrada test` con Docker activo para cubrir también
  Testcontainers.
