# Diseño: corregir tope del lote por tanda

## Decisión: contar todas las membresías asignadas a la tanda

`maxPersonas` se interpreta como el tamaño total del lote de una tanda, no como
el número de personas simultáneamente `HABILITADO`. La consulta nueva cuenta
por `(idEvento, idTanda)` sin filtro de estado:

```java
select count(i) from IngresoFila i
where i.idEvento = :idEvento and i.idTanda = :idTanda
```

Los estados terminales conservan `idTanda`: `COMPRO` lo asigna la habilitación y
nunca se limpia; `VENCIDO`/`EN_ESPERA` por cupo cambian estado y `venceEn`, pero
no la tanda; `SALIO` y `CANCELADO` tampoco la borran. Por eso el conteo por
tanda es estable y no depende de cuántos ya compraron.

No se cambian los conteos de `HABILITADO` de `CompraService.java:774` ni de
`FilaVirtualService.java:299,330`: allí se pregunta si quedan turnos por
resolver para cerrar una tanda, no cuántas personas tiene el lote.

## Decisión: arreglo del 500 sin cambiar el contrato

`convertirIngreso` usaba un ternary anidado cuyo tipo se promovía a `long`;
cuando la rama final era `null`, el unboxing lanzaba
`NullPointerException`. Se reemplaza por `if/else` con `Long`, de modo que
`COMPRO`, `VENCIDO`, `CANCELADO` y `SALIO` devuelvan `posicion: null` con HTTP
200. No se agrega ningún campo ni se cambia un código de estado.

## Decisión: agrupar ambos defectos en un change

Los dos defectos tienen la misma causa operativa (suposiciones sobre el estado
de la membresía al consultar/contar) y comparten archivo de servicio, tests y
ventana de verificación. Se corrigen juntos; la propuesta 2B de exponer
`estadoFila`/`motivo` sigue pendiente porque sí cambia el contrato de la API.
