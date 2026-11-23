# ADR-0005: Reutilizar cupo dentro del modelo por tandas

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Una Compra vencida puede devolver entradas al inventario y cupo a la tanda. Reaccionar habilitando inmediatamente a otra persona cambia el modelo de acceso por lotes y puede superar `maxPersonas`.

## Decisión

El cupo devuelto se ofrece a solicitudes reales de personas que ya tienen turno vigente en la tanda, en orden de admisión y mediante asignación atómica. No se habilita a una persona nueva en ese momento. El remanente no usado al cerrar la tanda se suma al cupo base de la siguiente.

## Alternativa descartada

Habilitar de inmediato a la siguiente persona en espera al liberar cupo. Aunque aprovecha antes el inventario, convierte la fila en un flujo continuo, rompe el modelo por tandas y el tope de personas habilitadas, y añade habilitaciones concurrentes entre instancias.

## Consecuencias

La tanda tiene cupo persistido y bloqueable. La próxima persona en espera no reserva capacidad hasta iniciar una compra; la disponibilidad real de asientos sigue siendo la última defensa.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
