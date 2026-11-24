# Tasks

## 1. Implementación en msvc-entrada

- [x] 1.1 Agregar en `CompraService` una dependencia opcional hacia `PendientePreferenciaRepository` mediante setter `@Autowired(required = false)`, sin cambiar el constructor existente; verificar que `mvn -pl msvc-entrada test-compile` compile sin errores.
- [x] 1.2 Agregar un helper privado que, para cada entrada pagada, cree un `PendientePreferencia` con `idUsuario = compra.getIdUsuario()`, `idEvento`, `idEntrada`, `correlationId` de la solicitud, estado `PENDIENTE` e `intentos = 0`, reutilizando `findByIdEntrada` para no duplicar; verificar que el helper use la misma lógica que `PagoService.registrarPendientePreferencia`.
- [x] 1.3 Invocar el helper dentro de `pagar(...)` en la misma transacción, después de marcar las entradas como `VALIDA` y antes de publicar `CompraPagadaEvent`; verificar que un pago rechazado o vencido no llegue a esa línea.

## 2. Prueba de regresión (sin modificar tests existentes)

- [x] 2.1 Crear `CompraPagoPreferenciaTest` con mocks, siguiendo el patrón de `CompraEventoPublicacionTest`, que confirme un pago aprobado con dos entradas y capture los `PendientePreferencia` guardados; verificar que la aserción exige dos avisos, `idUsuario = 10` (el comprador) e `idEntrada` distintos.
- [x] 2.2 Agregar casos al mismo test para pago rechazado y para aviso ya existente; verificar que `save` no se llame en ninguno de los dos.
- [x] 2.3 Ejecutar `mvn -pl msvc-entrada test` y verificar que todos los tests pasan, incluidos los 7 que construyen `CompraService` sin el repository. (243 tests, 0 fallos, 0 errores, 6 skipped — BUILD SUCCESS)

## 3. Verificación E2E

- [x] 3.1 Reconstruir la imagen de `msvc-entrada` y reiniciar el gateway; verificar que Eureka registre las dos instancias y `/actuator/health` devuelva UP.
- [x] 3.2 Ejecutar la colección `Entrega4/postman/TicketYa-E2E.postman_collection.json` con Postman CLI; verificar 0 fallos, con H1/H4 devolviendo 200 (no 404) tras la compra. Resultado final: 0 fallos, H1 y H4 en 200. Las assertions pasan de 60 a 62 porque, al dar H1 200, se ejecutan las 2 condicionales 'es propio' y 'campos' que antes se omitían con el 404. El total de requests varía (42 en run10, 48 en el run final): el pre-request de H1 sondea el outbox asíncrono con `pm.sendRequest` y hace tantos intentos como tarde el worker (1 entrada cada 5 s, más la cola de corridas anteriores); son requests de sondeo, no requests nuevas de la colección, que sigue teniendo 40.
- [x] 3.3 Consultar la BD de entrada y verificar que las entradas de la última compra pagada tienen fila en `pendientes_preferencia` con el `idUsuario` del comprador; verificar que no aparecen filas para los participantes invitados. Verificado: filas 11/12 (`idEntrada` 801/802, `idUsuario` 622, `ENVIADO`) y documento Mongo `_id=622` con `compras_procesadas=[801,802]`; solo 2 documentos en `preferencias_cliente` (los 2 compradores), ninguno para invitados.
