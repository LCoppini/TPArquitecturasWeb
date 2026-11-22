# Diseño: reportes de eventos para organizadores

## Decisión general

El reporte vive en `msvc-evento`, según `openspec/config.yaml`. Ese servicio es
dueño del evento, de sus sectores, de los nombres, de los precios y del estado
del evento, pero no de las ventas: para las cifras de entradas y dinero consulta
a `msvc-entrada` por Feign. El gateway solo autentica, autoriza por rol, fija
`X-User-Id` y enruta; no resuelve la pertenencia del recurso.

```
Cliente ORGANIZADOR
   |  GET /api/organizador/eventos/10/reporte + Bearer JWT
   v
msvc-gateway  -- JWT RS256, hasRole(ORGANIZADOR), X-User-Id del claim
   |  ruta genérica organizador-eventos-route -> lb://msvc-evento
   v
msvc-evento
   |-- valida evento.getIdOrganizador() == X-User-Id  -> 403 si no
   |-- SectorClient -> nombre y capacidadMax del sector (capacidad física)
   `-- EntradaClient -> GET /internal/entradas/eventos/10/reporte
                          |
                          v
                      msvc-entrada
                      GROUP BY sobre Entrada + RegistroPago + Pago
```

## Capacidad de sectores

### Estado verificado y corrección previa

En `msvc-ubicacion`:

- `UbicacionMapper.aEntidad()` **ya calcula en el servidor** la capacidad de un
  sector `NUMERADO` como `filas * asientosPorFila` y no lee
  `request.capacidadMax()` en esa rama.
- Pero `SectorRequest` todavía declara `@Positive Integer capacidadMax`, y
  `LugarServiceImpl.validarSectores()` la acepta si coincide con el producto.
  Es decir: **hoy se acepta del cliente**, aunque se valide que cuadre.
- `filas` y `asientosPorFila` son `@Positive` pero no `@NotNull`; la ausencia
  se detecta recién en la validación de servicio con
  `IllegalArgumentException`.

Corrección requerida **en un commit propio, con test**, antes de la
implementación del reporte:

- Los sectores `NUMERADO` **no aceptan `capacidadMax` en el request**: se
  rechaza con 400 si el cliente la envía, o se modela un request distinto que
  no la exponga. No basta con ignorarla en el mapper.
- `filas` y `asientosPorFila` son obligatorios y mayores a cero en `NUMERADO`.
- La única fuente de `capacidadMax` para `NUMERADO` sigue siendo el cálculo
  del servidor.
- El test fija que un request con `capacidadMax` distinto (o informado) se
  rechaza y que uno con `filas = 10`, `asientosPorFila = 20` persiste 200.

### Capacidad física, no capacidad del evento

- `EventoSector` tiene **solo** `idSector`, `evento` y `precio`. No guarda
  capacidad, por lo que no existe una capacidad local que contradiga a
  `SectorResponse.capacidadMax`.
- La capacidad que usa el reporte es la **física del sector**, tal como la
  calcula o informa `msvc-ubicacion`. Evento no la recalcula ni la sobrescribe.
- Precio y capacidad son conceptos distintos: el precio vive en
  `EventoSector.precio`, la capacidad en `SectorResponse.capacidadMax`.

### Test de sector numerado

El test del reporte arma un sector `NUMERADO` con `filas = 10` y
`asientosPorFila = 20`, hace que `SectorClient` devuelva `capacidadMax = 200`
(lo que calculó `msvc-ubicacion`) y verifica que, con 120 entradas activas,
`porcentajeOcupacion` sea `60.00`. El reporte **no multiplica filas por
asientos**: consume `capacidadMax` ya calculada; el test comprueba que esa es
la base del cálculo.

## Contrato interno

Se unifica el contrato muerto `/eventos/{idEvento}/ventas` con el nuevo
reporte:

- `GET /internal/entradas/eventos/{idEvento}/reporte`.
- No lleva prefijo `/api/**`, por lo que el gateway no lo enruta; además
  `/internal/**` está denegado explícitamente en `SecurityConfig`.
- Respuesta: `{ idEvento, porSector[], totales }`.
- Cada item de `porSector` tiene `idSector`, `entradasEmitidas`,
  `entradasActivas`, `entradasDevueltas`, `enReintegro`, `recaudacionBruta`,
  `recaudacionDevuelta` y `recaudacionNeta`.
- `idEvento` del body debe coincidir con el `idEvento` pedido; si no coincide,
  se trata como respuesta incompleta.
- `totales.entradasActivas` reemplaza al campo usado hoy por
  `cantidadVendida()` para el orden por relevancia. Las devueltas no deben
  incidir en ese orden.

## Conteo de entradas

La agrupación se hace por `Entrada.idSector`.

| Estado de Entrada | Emitida | Activa | Devuelta | En reintegro | Ocupa |
|---|---:|---:|---:|---:|---:|
| `RESERVADA` | No | No | No | No | No |
| `VALIDA` | Sí | Sí | No | No | Sí |
| `USADA` | Sí | Sí | No | No | Sí |
| `EN_REINTEGRO` | Sí | No | No | Sí | No |
| `DEVUELTA` | Sí | No | Sí | No | No |

- `entradasEmitidas` = cantidad de entradas con `Emitida = Sí`.
- `entradasActivas` = cantidad de entradas con `Activa = Sí`.
- La ocupación se calcula solo con `entradasActivas`.

`EN_REINTEGRO` se informa pero no cuenta como ocupada porque su QR ya fue
invalidado. Decisión confirmada por el grupo.

## Recaudación

La fuente de dinero es `RegistroPago.montoTotal`, relacionado con la entrada
mediante `RegistroPago.idEntrada`. No se usa `EventoSector.precio`, porque ese
precio puede haber cambiado después de la compra.

`RegistroPago.montoTotal` es **por entrada**, no por compra:

- `Pago` guarda un único `idEntrada`.
- `RegistroPagoId` es la clave compuesta `(idEntrada, idPago)`.
- `PagoService.crear()` asigna `montoTotal = entrada.getPrecio()`.

Por lo tanto, dos entradas aprobadas para el mismo usuario y evento generan dos
`Pago` y dos `RegistroPago`, y la recaudación debe ser la suma de ambos. No hay
hoy un pago agrupado que pueda duplicarse; se agrega igual un test que fije esa
suma para proteger el contrato si apareciera un agregado de compra.

| Estado de Pago | Bruta | Devuelta | Neta |
|---|---:|---:|---:|
| `APROBADO` | Sí | No | Sí |
| `REINTEGRO_PENDIENTE` | Sí | No | Sí |
| `REEMBOLSADO` | Sí | Sí | No |
| Otros | No | No | No |

Regla anti doble descuento: una entrada `DEVUELTA` con pago
`REINTEGRO_PENDIENTE` sigue en bruta y neta; recién pasa a
`recaudacionDevuelta` cuando el pago está `REEMBOLSADO`. El descuento de dinero
lo decide el estado del pago, no el estado de la entrada.

`recaudacionNeta = recaudacionBruta - recaudacionDevuelta`.

Tipos y formato: montos y porcentajes `BigDecimal(12,2)`, redondeo `HALF_UP` a
2 decimales, JSON numérico sin símbolo de moneda, conteos `Long`, capacidad
`Integer`.

## Verificación del mock de pagos

El mock existente sí puede llevar un pago a `REEMBOLSADO`:

- Con `PASARELA_RESULTADO=APRUEBA`, `reintegrarEntrada()` y
  `cancelarEvento()` marcan el pago como `REEMBOLSADO` y la entrada como
  `DEVUELTA`.
- Con `FALLA`, `reintegroFalla()` devuelve `true`: `cancelarEvento()` deja el
  pago en `REINTEGRO_PENDIENTE` y la entrada en `EN_REINTEGRO`. No hay
  `recaudacionDevuelta` y la neta se mantiene completa.
- `PagoServiceTest.dejaReintegroPendienteSiFallaYCompletaElReintentoUnaSolaVez`
  ya cubre la transición a nivel de pago.

Dos pruebas nuevas a nivel reporte:

1. Cancelación con mock en `FALLA`: `estadoEvento = "CANCELADO"`, entradas en
   `EN_REINTEGRO`, pagos `REINTEGRO_PENDIENTE`, `recaudacionDevuelta = 0` y
   `recaudacionNeta` igual a la bruta.
2. Regla `DEVUELTA + REINTEGRO_PENDIENTE`: **no es alcanzable por el flujo real
   del mock**, porque con `FALLA` la entrada queda `EN_REINTEGRO`. Se conserva
   como prueba de la regla con datos armados a mano. El nombre del test y un
   comentario deben dejar explícito que es un fixtures artificial y no un
   estado que produzca `cancelarEvento()`.

No se duplica la prueba de transición del mock, que ya existe en
`PagoServiceTest`.

## Agregación en msvc-entrada

Para evitar una consulta por cada estado, el service de reporte agrega en base:

- Entradas: `GROUP BY idSector, estado`, contando filas por estado. Desde esos
  grupos se derivan `entradasEmitidas`, `entradasActivas`,
  `entradasDevueltas` y `enReintegro` según la tabla de conteo.
- Dinero: `GROUP BY idSector, estadoPago`, sumando
  `RegistroPago.montoTotal` uniendo `Entrada -> RegistroPago -> Pago`. Desde
  esos grupos se derivan bruta, devuelta y neta según la tabla de pagos.
- Los dos resultados se combinan por `idSector` en memoria para producir los
  DTOs; no se consulta el repositorio una vez por estado.
- Conjuntos vacíos producen `porSector = []` y totales en 0/0.00.
- Esta decisión es de implementación: las specs describen el resultado, no la
  forma de la query, pero `tasks.md` la registra como requisito.

## Cálculo en msvc-evento

- Nombre y capacidad se obtienen con `SectorClient` existente, usando
  `SectorResponse.capacidadMax`.
- `porcentajeOcupacion = entradasActivas * 100 / capacidad`, con `HALF_UP` a
  2 decimales.
- `totales.porcentajeOcupacion = totalActivas * 100 / totalCapacidad`. Si
  `totalCapacidad` es 0, se reporta `0.00`.
- La lista `porSector` se arma con los sectores del evento más cualquier
  `idSector` devuelto por Entrada.
- **Sector con ventas al que `SectorClient` responde 404**: significa que el
  sector fue borrado y no hay FK entre servicios. Se incluye el item con
  `capacidad = 0`, `nombreSector = null`, `porcentajeOcupacion = 0.00` y sus
  conteos y recaudación intactos. No se descarta la fila ni se propaga el 404.
- **Error de red o 5xx de `SectorClient`**: se responde 502 y no se devuelve
  ningún reporte parcial. La distinción la hace el código de estado de
  `FeignException`: 404 es dato ausente aceptado; cualquier otro fallo es
  error del proveedor.
- Si `capacidad` es `null` o 0 en un sector vivo, se reporta `0.00` sin
  perder sus cifras.
- `estadoEvento` se toma del evento y se expone en el contrato público.
- Un evento cancelado responde 200 con el snapshot final; el descuento de
  dinero solo incluye pagos `REEMBOLSADO`, nunca `REINTEGRO_PENDIENTE`.
- Pertenencia: se compara `evento.getIdOrganizador()` contra `X-User-Id`. Si el
  evento existe pero pertenece a otro organizador, se lanza
  `AccesoDenegadoException` (403). Si no existe, 404. Si `EntradaClient` falla
  o el body interno está incompleto, `ServicioExternoException` (502).

## Errores

| Situación | Respuesta |
|---|---|
| Organizador dueño, evento vigente o cancelado | 200 con el JSON completo |
| Evento existente de otro organizador | 403, sin cifras en el body |
| Evento inexistente | 404 |
| `idEvento` no numérico | 400 |
| `X-User-Id` ausente o no numérico | 403, nunca 500 por `NumberFormatException` |
| `SectorClient` 404 (sector borrado) | 200, item con capacidad 0 y nombre nulo |
| `SectorClient` error de red o 5xx | 502, sin cifras parciales |
| Fallo de `EntradaClient` o body interno incompleto | 502, sin cifras parciales |
| `/internal/**` sin token en el gateway | 401 con formato uniforme |
| `/internal/**` con token en el gateway | 403 (`denyAll`) con formato uniforme |

El manejo de `X-User-Id` debe convertir el header a `String` y validarlo antes
de parsear, o mapear `MissingRequestHeaderException` y
`MethodArgumentTypeMismatchException` a 403 para ese header. No se puede
depender del 400 por defecto de Spring ni dejar escapar un
`NumberFormatException`. El `@PathVariable Long idEvento` no numérico sí se
mantiene como 400.

Se mantiene 403 para evento ajeno y 404 para inexistente porque esa es la
convención ya vigente en `identidad-servicios` y en `msvc-evento`
(`AccesoDenegadoException`). Cambiar a 404 sería un cambio de política global,
no puntual del reporte.

El gateway mantiene su formato uniforme `{status, error, message, path}` para
401 y 403. Los 400/403/404/502 de los servicios usan su manejo de excepciones
existente o el que se agregue para los headers.

## Cambios en el gateway

**No se agrega una ruta nueva.** Ya existe en ambos archivos de configuración:

```yaml
- id: organizador-eventos-route
  uri: lb://msvc-evento
  predicates:
    - Path=/api/organizador/eventos/**
```

Ese prefijo ya tiene `hasRole("ORGANIZADOR")` en `GatewayRoleMatrix`, por lo
que `/api/organizador/eventos/{idEvento}/reporte` queda cubierto sin tocar el
YAML.

`SecurityConfig` ya declara `.requestMatchers("/internal/**").denyAll()` y la
cadena alcanza `/api/**` y `/internal/**`. La tarea es **especificar y probar**
el resultado observable: 401 sin token y 403 con token, ambos con el formato
uniforme. El caso con token ya existe en `GatewayRoleMatrixTest`
(`contrato interno siempre denegado`); se agrega el caso sin token en
`GatewayErrorHandlingTest`, donde ya viven `sinTokenEnRutaProtegidaResponde401`
y el formato de 403.

En `GatewayRoleMatrixTest` se agregan solo casos de rol del reporte. Los casos
de identidad no van ahí: ya están en `GatewayErrorHandlingTest`
(`elBackendRecibeElIdUsuarioDelClaimDelJwt`,
`elBackendNoRecibeXUserRoleAunqueElClienteLoEnvie` y
`elXUserIdDelClienteEsSobrescritoPorElDelToken`).

## Identidad

- El gateway reemplaza cualquier `X-User-Id` del cliente por el claim
  `idUsuario` del JWT y descarta `X-User-Role`.
- Los servicios, controllers y Feign siguen recibiendo solo `X-User-Id`.
- No se agrega ninguna llamada desde Evento a `msvc-usuarios` para consultar
  roles. La pertenencia se resuelve comparando el dueño del evento.
- Un `X-User-Id` ausente o no numérico se responde 403 en el servicio, nunca
  500.

## Estrategia de pruebas y archivado

Según `openspec/config.yaml` y ADR-0002, la integración E2 se hace con
`docker-compose` y no con Testcontainers nuevo. Para este change se decide:

- **La verificación de integración se mueve a EP-21.** La tarea de seguimiento
  del change `verificacion-e2e-carga` debe referenciar `reportes-organizador`
  y ejecutar el flujo del reporte con el compose levantado.
- **Este change se archiva al pasar los tests unitarios de `msvc-entrada`,
  `msvc-evento`, `msvc-ubicacion` (por la corrección de capacidad) y los tests
  de gateway.** No se espera a EP-21 para archivarlo.
- No se sustituye la integración diferida por Testcontainers nuevo.
- `NoDobleVentaTest` queda intacto; no se elimina ni se edita para que pase.
- Si un test existente queda obsoleto por el cambio de contrato de
  `cantidadVendida()`, se reemplaza en commit separado y se registra en
  `tasks.md`, sin relajar la aserción.

## Dependencia previa

La implementación no comienza hasta que se cumplan **las dos** condiciones:

1. El **smoke test de login real** esté verde: emisión RS256 en
   `msvc-usuarios`, validación en el gateway y llegada de `X-User-Id` al
   servicio destino. Sin eso, las pruebas del reporte podrían ocultar un fallo
   de integración de identidad.
2. Esté integrado el **commit de `msvc-evento` de separación de paths por
   rol**, porque toca `organizador-eventos-route` y la eliminación de
   `validarRol`. Sin ese commit, los cambios del gateway y del controller del
   reporte podrían pisarse o quedar inconsistentes.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Calcular recaudación con `precio * emitidas` | Ignora el importe cobrado y los reintegros. |
| Descontar por `Entrada.DEVUELTA` y por `Pago.REEMBOLSADO` | Doble descuento. |
| Usar `entradasEmitidas` para ocupación o relevancia | Incluiría devueltas y en reintegro como vigentes. |
| Agregar `organizador-reportes-route` | La ruta genérica existente ya llega a `msvc-evento`; sería duplicar configuración. |
| Exponer ventas bajo `/api/entradas/**` | Mezcla pertenencia del organizador con datos de entradas y rompe el contrato interno. |
| Crear `msvc-reportes` nuevo | EP-18 lo permitiría, pero `config.yaml` ya decidió que vive en Evento. |
| Tomar capacidad de `EventoSector` | La entidad no tiene ese campo; la capacidad es la física del sector. |
| Recalcular `filas * asientosPorFila` en Evento | `msvc-ubicacion` ya lo calcula; duplicaría la fuente de verdad. |
| Descartar el sector ante un 404 de `SectorClient` | Perdería cifras válidas de un sector borrado sin FK. |
| Confundir 404 y 5xx de `SectorClient` | 404 es dato ausente; 5xx es fallo del proveedor y debe ser 502. |
| Una consulta por estado de entrada/pago | Se reemplaza por `GROUP BY` para no multiplicar llamadas. |
| Testcontainers para la integración nueva | ADR-0002 eligió docker-compose como entorno de integración E2. |
| Agregar un ADR | No hay tecnología ni patrón nuevo: Feign, JPA y DTOs ya están en uso. |

## Riesgos

- `cantidadVendida()` ahora invoca el **reporte completo** desde la búsqueda
  por relevancia: una llamada con join a pagos por cada evento. Si la medición
  muestra lentitud, se agregará un endpoint interno liviano que devuelva solo
  `totales.entradasActivas`; no se hace en este change.
- Varias llamadas a `SectorClient` si el evento tiene muchos sectores. Se
  acepta por ahora; cachear capacidades queda fuera de alcance.
- Feign sin circuit breaker: un fallo se propaga como 502. EP-13 lo mejorará
  después.
- El cambio del contrato de ventas obliga a ajustar `cantidadVendida()`. Hoy no
  hay tests que referencien el path viejo; se agregan nuevos en commit separado
  sin relajar aserciones existentes.
- La corrección de `SectorRequest` en `msvc-ubicacion` puede afectar requests
  existentes que envíen `capacidadMax` en sectores numerados; se documenta en
  el commit propio con su test.
- La integración E2 queda como deuda visible en EP-21; el archivado de este
  change no implica que esa verificación ya se haya ejecutado.

## ADR

No corresponde uno nuevo. Se apoya en ADR-0001 (gateway autentica, servicio
valida pertenencia), ADR-0002 (pruebas E2 con docker-compose) y ADR-0003
(identidad por `X-User-Id`).
