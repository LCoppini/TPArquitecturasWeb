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
   |-- SectorClient -> nombre y capacidadMax por sector
   `-- EntradaClient -> GET /internal/entradas/eventos/10/reporte
                          |
                          v
                      msvc-entrada
                      Entrada + RegistroPago + Pago
```

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

Tipos y formato:

- Montos y porcentajes: `BigDecimal(12,2)`.
- Redondeo: `HALF_UP` a 2 decimales.
- JSON numérico, sin símbolo de moneda.
- Misma moneda implícita que `EventoSector.precio`.
- Conteos: `Long`. Capacidad: `Integer`.

## Verificación del mock de pagos

El mock existente sí puede llevar un pago a `REEMBOLSADO`:

- Con `PASARELA_RESULTADO=APRUEBA`, `reintegrarEntrada()` y
  `cancelarEvento()` marcan el pago como `REEMBOLSADO` y la entrada como
  `DEVUELTA`.
- Con `FALLA`, el pago queda `REINTEGRO_PENDIENTE` y la entrada sigue en
  `EN_REINTEGRO`.
- `PagoServiceTest.dejaReintegroPendienteSiFallaYCompletaElReintentoUnaSolaVez`
  ya cubre la transición a nivel de pago.

El reporte agrega una prueba de nivel superior: un pago `REEMBOLSADO` debe
aparecer en `recaudacionDevuelta` y no en `recaudacionNeta`. No se duplica la
prueba del mock.

## Cálculo en msvc-evento

- Capacidad y nombre del sector se obtienen con el `SectorClient` existente,
  usando `SectorResponse.capacidadMax`.
- `porcentajeOcupacion = entradasActivas * 100 / capacidad`, con `HALF_UP` a
  2 decimales.
- `totales.porcentajeOcupacion = totalActivas * 100 / totalCapacidad`. Si
  `totalCapacidad` es 0, se reporta `0.00`.
- Si `capacidad` es `null` o 0 en un sector, se reporta `0.00`.
- La lista `porSector` se arma con los sectores del evento más cualquier
  `idSector` devuelto por Entrada. Si un sector tiene entradas emitidas pero no
  tiene capacidad configurada, se incluye con `capacidad: 0` y
  `porcentajeOcupacion: 0.00`.
- `estadoEvento` se toma del evento y se expone en el contrato público para que
  el cliente distinga un evento cancelado.
- Un evento cancelado responde 200 con el snapshot final: entradas devueltas,
  pagos reembolsados y recaudación neta ya descontada.
- Pertenencia: se compara `evento.getIdOrganizador()` contra `X-User-Id`. Si el
  evento existe pero pertenece a otro organizador, se lanza
  `AccesoDenegadoException` (403). Si no existe, 404. Si `SectorClient` o
  `EntradaClient` fallan, o el body interno está incompleto,
  `ServicioExternoException` (502), igual que en `cantidadVendida()`.

## Errores

| Situación | Respuesta |
|---|---|
| Organizador dueño, evento vigente o cancelado | 200 con el JSON completo |
| Evento existente de otro organizador | 403, sin cifras en el body |
| Evento inexistente | 404 |
| `idEvento` no numérico | 400 |
| Falta `X-User-Id` en el servicio | 403, no 500 |
| Fallo de Feign o body interno incompleto | 502, sin cifras parciales |

Se mantiene 403 para evento ajeno y 404 para inexistente porque esa es la
convención ya vigente en `identidad-servicios` y en `msvc-evento`
(`AccesoDenegadoException`). Cambiar a 404 sería un cambio de política global,
no puntual del reporte.

El gateway mantiene su formato uniforme `{status, error, message, path}` para
401 y 403. Los 403/404/502 de los servicios usan su manejo de excepciones
existente.

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

Lo único que se agrega en el gateway son casos de prueba de rol en
`GatewayRoleMatrixTest`. Los casos de identidad no van ahí: ya están en
`GatewayErrorHandlingTest` (`elBackendRecibeElIdUsuarioDelClaimDelJwt`,
`elBackendNoRecibeXUserRoleAunqueElClienteLoEnvie` y
`elXUserIdDelClienteEsSobrescritoPorElDelToken`).

## Identidad

- El gateway reemplaza cualquier `X-User-Id` del cliente por el claim
  `idUsuario` del JWT.
- El gateway descarta `X-User-Role`.
- Los servicios, controllers y Feign siguen recibiendo solo `X-User-Id`.
- No se agrega ninguna llamada desde Evento a `msvc-usuarios` para consultar
  roles. La pertenencia se resuelve comparando el dueño del evento.

## Estrategia de pruebas

Según `openspec/config.yaml` y ADR-0002:

- Las pruebas unitarias se ejecutan con mocks de repositorio y de Feign.
- La prueba de integración se ejecuta contra el entorno levantado con
  `docker-compose`, no con Testcontainers nuevo.
- `NoDobleVentaTest` queda intacto; no se elimina ni se edita para que pase.
- Esta verificación depende de que EP-21/`verificacion-e2e-carga` deje el
  compose con los servicios levantados. Hasta entonces, la tarea queda como
  compuerta y no se sustituye por Testcontainers.

## Dependencia previa

La implementación no comienza hasta que el **smoke test de login real** esté
verde: emisión RS256 en `msvc-usuarios`, validación en el gateway y llegada de
`X-User-Id` al servicio destino. Sin eso, las pruebas del reporte podrían
ocultar un fallo de integración de identidad.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Calcular recaudación con `precio * emitidas` | Ignora el importe cobrado y los reintegros. |
| Descontar por `Entrada.DEVUELTA` y por `Pago.REEMBOLSADO` | Doble descuento. |
| Usar `entradasEmitidas` para ocupación o relevancia | Incluiría devueltas y en reintegro como vigentes. |
| Agregar `organizador-reportes-route` | La ruta genérica existente ya llega a `msvc-evento`; sería duplicar configuración. |
| Exponer ventas bajo `/api/entradas/**` | Mezcla pertenencia del organizador con datos de entradas y rompe el contrato interno. |
| Crear `msvc-reportes` nuevo | EP-18 lo permitiría, pero `config.yaml` ya decidió que vive en Evento. |
| Testcontainers para la integración nueva | ADR-0002 eligió docker-compose como entorno de integración E2. |
| Agregar un ADR | No hay tecnología ni patrón nuevo: Feign, JPA y DTOs ya están en uso. |

## Riesgos

- Varias llamadas a `SectorClient` si el evento tiene muchos sectores. Se
  acepta por ahora; cachear capacidades queda fuera de alcance.
- Feign sin circuit breaker: un fallo se propaga como 502. EP-13 lo mejorará
  después.
- El cambio del contrato de ventas obliga a ajustar `cantidadVendida()`. Hoy no
  hay tests que referencien el path viejo; se agregan nuevos en commit separado
  sin relajar aserciones existentes.
- La integración depende de EP-21; se registra como compuerta, no se resuelve
  acá.

## ADR

No corresponde uno nuevo. Se apoya en ADR-0001 (gateway autentica, servicio
valida pertenencia), ADR-0002 (pruebas E2 con docker-compose) y ADR-0003
(identidad por `X-User-Id`).
