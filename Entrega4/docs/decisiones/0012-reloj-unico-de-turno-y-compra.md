# ADR-0012: La Compra hereda el reloj del turno

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

La fila da a cada persona habilitada diez minutos para iniciar y completar su compra. Si la Compra iniciara un reloj nuevo, una persona podría extender el acceso creando una reserva al final de su turno y quedar con dos vencimientos concurrentes.

## Decisión

Una Compra iniciada durante un turno hereda exactamente `venceEn` de ese turno. Reservar no inicia ni extiende un plazo. Una Compra en venta libre posterior a un cierre manual no tiene turno de origen y recibe una reserva ordinaria de diez minutos desde su creación. El pago y el vencimiento se resuelven mediante actualización condicional para que solo uno prevalezca.

## Alternativa descartada

Mantener un reloj de diez minutos para el turno y comenzar un segundo reloj al crear Compra. Se descarta porque extiende el tiempo de acceso, puede dejar desalineados el turno y sus reservas, y agrega estados de vencimiento concurrentes innecesarios.

## Consecuencias

La Compra debe guardar su vencimiento y el turno de origen cuando corresponda. Los procesos de pago y compensación deben validar el mismo instante límite; la venta libre conserva el límite ordinario sin modificar turnos ya vigentes.

## Referencias

- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/specs/compra-agrupada/spec.md`
- `openspec/changes/fila-virtual/design.md`
