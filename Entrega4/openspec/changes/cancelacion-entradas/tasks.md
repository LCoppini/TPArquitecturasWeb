# Tareas

- [x] Agregar la transición de reservas `RESERVADA` a `CANCELADA` y limpiar su
      expiración.
- [x] Mantener la transición de entradas `VALIDA` y `USADA` a `EN_REINTEGRO`.
- [x] Filtrar los pagos elegibles para reintegro y conservar estados recuperables
      ante fallo del mock.
- [x] Agregar prueba de integración de cancelación y liberación de disponibilidad.
- [x] Agregar pruebas unitarias de pago posterior a cancelación y de reintegro.
- [ ] Revisar propuesta, requisitos y decisiones con el grupo antes de considerar
      el cambio aprobado.
- [x] Ejecutar `mvn -pl msvc-entrada test` con Docker activo: 11 tests ejecutados,
      0 fallos y 0 errores.
- [ ] Confirmar por separado el alcance y la implementación de notificaciones
      (US-4 / EP-14).
- [ ] Antes de integrar una pasarela real, definir idempotencia de reintegros con
      el proveedor y agregar pruebas de fallos de red.
