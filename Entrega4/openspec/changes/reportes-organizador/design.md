# Diseño: reportes de eventos para organizadores

## Decisión general

El reporte vive en `msvc-evento`, según `openspec/config.yaml`. Ese servicio es
dueño del evento, de sus sectores, de los nombres y de los precios, pero no de
las ventas: para las cifras de entradas y dinero consulta a `msvc-entrada` por
Feign. El gateway solo autentica, autoriza por rol, fija `X-User-Id` y enruta;
no resuelve la pertenencia del recurso.

```
Cliente ORGANIZADOR
   |  GET /api/organizador/eventos/10/reporte + Bearer JWT
   v
msvc-gateway  -- JWT RS256, hasRole(ORGANIZADOR), X-User-Id del claim
   |  lb://msvc-evento
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
- Cada item de `porSector` tiene `idSector`, `entradasVendidas`,
  `entradasDevueltas`, `enReintegro`, `recaudacionBruta`,
  `recaudacionDevuelta` y `recaudacionNeta`.
- `idEvento` del body debe coincidir con el `idEvento` pedido; si no coincide,
  se trata como respuesta incompleta.
- `totales.entradasVendidas` reemplaza al campo usado hoy por
  `cantidadVendida()` para el orden por relevancia.

## Conteo de entradas

La agrupación se hace por `Entrada.idSector`.

| Estado de Entrada | Vendida | Devuelta | En reintegro | Ocupa |
|---|---:|---:|---:|---:|
| `RESERVADA` | No | No | No | No |
| `VALIDA` | Sí | No | No | Sí |
| `USADA` | Sí | No | No | Sí |
| `EN_REINTEGRO` | No | No | Sí | No |
| `DEVUELTA` | No | Sí | No | No |

`EN_REINTEGRO` se informa pero no cuenta como ocupada porque su QR ya fue
invalidado. Decisión confirmada por el grupo.

## Recaudación

La fuente de dinero es `RegistroPago.montoTotal`, relacionado con la entrada
mediante `RegistroPago.idEntrada`. No se usa `EventoSector.precio`, porque ese
precio puede haber cambiado después de la compra.

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

## Cálculo en msvc-evento

- Capacidad y nombre del sector se obtienen con el `SectorClient` existente,
  usando `SectorResponse.capacidadMax`.
- `porcentajeOcupacion = entradasVendidas * 100 / capacidad`, con `HALF_UP` a
  2 decimales.
- Si `capacidad` es `null` o 0, se reporta `0.00`.
- `totales.porcentajeOcupacion = totalVendidas * 100 / totalCapacidad`. Si
  `totalCapacidad` es 0, se reporta `0.00`.
- La lista `porSector` se arma con los sectores del evento más cualquier
  `idSector` devuelto por Entrada. Si un sector tiene ventas pero no tiene
  capacidad configurada, se incluye con `capacidad: 0` y
  `porcentajeOcupacion: 0.00`.
- Pertenencia: se compara `evento.getIdOrganizador()` contra `X-User-Id`. Si
  difieren, se lanza `AccesoDenegadoException` (403). Si el evento no existe,
  404. Si `SectorClient` o `EntradaClient` fallan, o el body interno está
  incompleto, `ServicioExternoException` (502), igual que en
  `cantidadVendida()`.

## Snapshot

El reporte no recibe fechas. Refleja el estado actual de entradas y pagos al
momento de la consulta. Si se necesitara evolución histórica, sería otro change
con otra tabla de eventos de reporte.

## Errores

| Situación | Respuesta |
|---|---|
| Organizador dueño | 200 con el JSON completo |
| Otro organizador | 403, sin cifras en el body |
| Evento inexistente | 404 |
| `idEvento` no numérico | 400 |
| Falta `X-User-Id` en el servicio | 403, no 500 |
| Fallo de Feign o body interno incompleto | 502, sin cifras parciales |

El gateway mantiene su formato uniforme `{status, error, message, path}` para
401 y 403. Los 403/404/502 de los servicios usan su manejo de excepciones
existente.

## Cambios en el gateway

Se agrega antes de la ruta genérica:

```yaml
- id: organizador-reportes-route
  uri: lb://msvc-evento
  predicates:
    - Path=/api/organizador/eventos/{idEvento}/reporte
```

- Se replica en `msvc-gateway/src/main/resources/application.yml` y en
  `msvc-config/src/main/resources/configuration/msvc-gateway.yml`.
- No hace falta un prefijo nuevo en `GatewayRoleMatrix`: el path empieza con
  `/api/organizador/eventos/**`, que ya tiene `hasRole("ORGANIZADOR")`.
- Sí se agrega un caso explícito en `GatewayRoleMatrixTest` con el path del
  reporte, aunque la regla ya cubra el prefijo.
- La prueba existente ya fallaría si apareciera una ruta nueva sin regla de
  rol; eso se conserva como red de seguridad.

## Identidad

- El gateway reemplaza cualquier `X-User-Id` del cliente por el claim
  `idUsuario` del JWT.
- El gateway descarta `X-User-Role`.
- Los servicios, controllers y Feign siguen recibiendo solo `X-User-Id`.
- No se agrega ninguna llamada desde Evento a `msvc-usuarios` para consultar
  roles. La pertenencia se resuelve comparando el dueño del evento.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Calcular recaudación con `precio * vendidas` | Ignora el importe cobrado y los reintegros. |
| Descontar por `Entrada.DEVUELTA` y por `Pago.REEMBOLSADO` | Doble descuento. |
| Exponer ventas bajo `/api/entradas/**` | Mezcla pertenencia del organizador con datos de entradas y rompe el contrato interno. |
| Crear `msvc-reportes` nuevo | EP-18 lo permitiría, pero `config.yaml` ya decidió que vive en Evento. |
| Agregar un ADR | No hay tecnología ni patrón nuevo: Feign, JPA y DTOs ya están en uso. |

## Riesgos

- Varias llamadas a `SectorClient` si el evento tiene muchos sectores. Se
  acepta por ahora; cachear capacidades queda fuera de alcance.
- Feign sin circuit breaker: un fallo se propaga como 502. EP-13 lo mejorará
  después.
- El cambio del contrato de ventas obliga a ajustar `cantidadVendida()` y sus
  tests; no se relaja ninguna aserción existente.

## ADR

No corresponde uno nuevo. Se apoya en ADR-0001 (gateway autentica, servicio
valida pertenencia) y ADR-0003 (identidad por `X-User-Id`).
