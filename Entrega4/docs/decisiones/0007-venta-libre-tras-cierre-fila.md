# ADR-0007: Mantener venta grupal libre después del cierre de la fila

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

Cerrar la fila es irreversible y retira a las personas que siguen esperando. Detener también todas las ventas podría dejar entradas disponibles sin vender, aun cuando el organizador ya decidió terminar la espera.

## Decisión

Al cerrar, los turnos vigentes pueden completarse hasta su vencimiento. Después, se habilita la Compra grupal sin turno, con el límite de seis entradas y control de disponibilidad real. Los endpoints individuales `POST /api/entradas` y `POST /api/pagos` siguen bloqueados para ese evento.

## Alternativa descartada

Detener la venta al cerrar la fila. Se descarta porque deja entradas sin vender una vez que la fila cumplió su objetivo.

## Consecuencias

El endpoint de Compra debe diferenciar fila cerrada con turnos todavía vigentes de venta libre posterior. El límite de cupo por tanda deja de aplicar al modo libre; el límite físico de inventario permanece.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/design.md`
