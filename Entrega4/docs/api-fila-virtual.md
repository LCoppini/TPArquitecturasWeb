# Contrato HTTP de fila virtual y Compra

Este documento es la fuente legible del contrato expuesto por `msvc-entrada`. Las
anotaciones `@Operation`/`@ApiResponses` de los controllers alimentan el OpenAPI
(`/v3/api-docs` del servicio) con los mismos códigos y descripciones. Los ejemplos
están tomados de los tests de controller y de integración citados al final.

## Acceso y cabeceras

El gateway enruta `/api/filas-virtuales/**` y `/api/compras/**` a `msvc-entrada`
para el rol `CLIENTE`, y `/api/organizador/filas-virtuales/**` para el rol
`ORGANIZADOR`. El cliente no puede elegir identidad: el gateway valida el JWT,
descarta `X-User-Role` y fija `X-User-Id` desde el claim `idUsuario`.

| Cabecera | Quién la pone | Uso |
|---|---|---|
| `Authorization: Bearer <JWT>` | Cliente | Autenticación en el gateway. |
| `X-User-Id` | Gateway | Identidad del solicitante en el servicio. |
| `Idempotency-Key` | Cliente | Clave de la Compra o del pago; repetirla con el mismo cuerpo devuelve el mismo resultado. |
| `X-Correlation-Id` | Cliente o gateway | El gateway la genera si falta o está en blanco, la reenvía al servicio y la devuelve en la respuesta, incluso en errores. |

## Fila virtual

### `PUT /api/organizador/filas-virtuales/eventos/{idEvento}/activacion`

Activa la fila. Sin cuerpo usa los límites por defecto; con cuerpo:

```json
{ "maxPersonas": 100, "cupoEntradas": 4 }
```

`maxPersonas` y `cupoEntradas` son opcionales y deben ser positivos. Una fila ya
`ACTIVA` solo acepta la misma configuración; cualquier otro valor responde 409.

Respuesta 200 (`FilaEventoResponse`):

```json
{ "idEvento": 20, "estado": "ACTIVA", "maxPersonas": 100, "cupoEntradas": 4 }
```

Códigos: 200, 400, 403, 404, 409, 503 (flag `ticketya.fila-virtual.enabled=false`).

### `POST /api/organizador/filas-virtuales/eventos/{idEvento}/{operacion}`

Operaciones aceptadas: `PAUSAR`, `REANUDAR`, `REABRIR`. Cualquier otra transición
responde 409 con `Operación de fila no permitida`. Devuelve `FilaEventoResponse`.

Códigos: 200, 400, 403, 404, 409.

### `POST /api/filas-virtuales/eventos/{idEvento}/ingresos`

Crea la membresía del cliente o devuelve la vigente. La secuencia se asigna una
sola vez y es inmutable. Devuelve `IngresoFilaResponse`:

```json
{
  "idEvento": 20,
  "idIngreso": 5,
  "secuencia": 3,
  "estado": "HABILITADO",
  "posicion": 1,
  "venceEn": "2026-11-23T02:15:00Z",
  "idTanda": 7
}
```

`estado` puede ser `EN_ESPERA`, `HABILITADO`, `VENCIDO`, `SALIO` o `CANCELADO`.
Códigos: 200, 400, 404, 409, 503.

### `GET /api/filas-virtuales/eventos/{idEvento}/mi-estado`

Devuelve el mismo DTO para la membresía vigente del cliente. Códigos: 200, 400, 404.

## Compra

### `POST /api/compras`

Cuerpo (`CompraRequest`): `idEvento` y entre 1 y 6 `entradas`; cada entrada lleva
`idSector` y, para sectores numerados, `fila` y `asiento`.

```json
{
  "idEvento": 20,
  "entradas": [
    { "idSector": 30, "fila": null, "asiento": null },
    { "idSector": 30, "fila": null, "asiento": null }
  ]
}
```

Respuesta 201 (`CompraResponse`) con `idCompra`, `estado=RESERVADA`, `total`,
`venceEn`, `entradas` y `codigo=null`. La reserva descuenta el cupo de la tanda y
crea las entradas en una sola transacción; si falla, no queda ni cupo descontado
ni entrada huérfana.

Contrato de idempotencia y cupo:

- Misma `Idempotency-Key` y mismo cuerpo: se devuelve la Compra original (201) sin
  duplicar cargos ni entradas.
- Misma clave con cuerpo distinto: 422.
- Sin turno vigente o con una Compra `RESERVADA` pendiente: 409 con
  `codigo=COMPRA_PENDIENTE`.
- Último cupo agotado por otra solicitud concurrente: 409 con
  `codigo=CUPO_TANDA_INSUFICIENTE` y `cantidadDisponible=0`; se registra el rechazo
  y la tanda se cierra. Quien ya estaba habilitado vuelve a `EN_ESPERA` con su
  secuencia original.
- Con el flag deshabilitado: 503, incluso para un evento con fila persistida
  cerrada manualmente (no se habilita la venta libre por este camino).

Códigos: 201, 400, 409, 422, 503.

### `POST /api/compras/{idCompra}/pago`

Cuerpo (`PagoCompraRequest`): `{ "medioDePago": "TARJETA" }`. Respuesta 200
(`PagoCompraResponse`) con `idPago`, `idCompra`, `idUsuario`, `estado`,
`medioDePago`, `total` y `referenciaPasarela`. `estado` es el estado **agregado de
la Compra**, no el interno del Pago.

- `APRUEBA`: Compra `PAGADA`, una entrada por ítem, un QR y un `RegistroPago` por
  entrada, y un único `CompraConfirmada` con todos los `idEntrada`.
- `FALLA`: Compra `RECHAZADA`, Pago `FALLIDO`, entradas canceladas, cupo devuelto,
  `venceEn` sin extender y sin QR; permite un nuevo intento.
- `DEMORA`: Compra sigue `RESERVADA` y Pago `PENDIENTE` a la espera de conciliación.
- Si la Compra vence primero: 409, sin QR ni emisión.

Códigos: 200, 400, 404, 409, 422.

### `GET /api/compras/{idCompra}` y `POST /api/compras/{idCompra}/devolucion`

La consulta devuelve 200 para la Compra propia y 404 si es ajena o inexistente; no
muta nada. La devolución responde 202 y solo se admite con Compra `PAGADA`, sin
entradas usadas y al menos 72 horas antes del evento. El reintegro y sus reintentos
usan una única clave `devolucion-compra-{idCompra}`, por lo que repetirlos no
duplica el reintegro.

Códigos de la devolución: 202, 400, 404, 409.

## Verificación de los ejemplos

Los ejemplos y códigos de este documento se contrastan con la implementación en:

- `FilaVirtualControllerTest` y `CompraControllerTest` (MockMvc): DTOs, cabeceras
  `X-User-Id`/`Idempotency-Key` y códigos 200/201/202/400/404/409/503.
- `FilaVirtualServiceTest`: transiciones `PAUSAR`/`REANUDAR`/`REABRIR`, rechazo de
  operaciones no listadas y flag deshabilitado.
- `CompraConcurrenciaIntegrationTest`: cupo insuficiente, clave reutilizada con
  otro payload (422), vencimiento antes del pago, carrera cierre-creación y pagos
  con seis entradas/QRs/registros.
- `DocumentacionApiTest`: comprueba que los ejemplos del documento aparecen en las
  anotaciones OpenAPI de los controllers y que la matriz de roles del gateway
  cubre los tres prefijos aquí documentados.
