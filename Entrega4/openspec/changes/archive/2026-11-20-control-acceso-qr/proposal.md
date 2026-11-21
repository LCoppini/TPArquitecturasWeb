# Propuesta: control de acceso con QR (Entrega 2)

## Por qué

La épica EP-16 exige que el personal de acceso valide entradas en la puerta sin
dejar pasar dos veces el mismo QR, que se rechace con el motivo correspondiente
y que cada ingreso quede registrado en el log de operaciones críticas.

Hoy el núcleo existe: `POST /api/accesos/escanear` está protegido con
`PERSONAL_ACCESO` en el gateway y `EntradaService.validarQr` hace un `UPDATE`
condicional atómico (`EntradaRepository.marcarUsada`). Pero ese comportamiento
no está cubierto por ningún test, no distingue por qué se rechaza un QR y no
deja rastro de quién escaneó: `AccesoController` recibe `X-User-Id` en el
parámetro `idPersonal` y no lo usa.

Además, la devolución aprobada y la cancelación de evento escriben el mismo
estado `EN_REINTEGRO`, por lo que el servicio solo puede responder "no está
vigente", sin el motivo que pide la consigna.

## Qué cambia

- Endpoint dedicado con DTO propio `QrEscaneoRequest` (`idEvento` y
  `codigoQr`), en lugar de reutilizar `EntradaRequest`, que exige `idSector` y
  expone campos ajenos al escaneo (`idUsuario`, `precio`, `claveIdempotencia`).
- Errores de QR con código estable: `QrError` (`QR_NO_EXISTE`,
  `QR_EVENTO_DISTINTO`, `QR_DEVUELTO`, `QR_EVENTO_CANCELADO`, `QR_YA_USADO`,
  `QR_VENCIDO`) y un body `{"error", "code"}`. Un QR inexistente o alterado
  devuelve **404 `QR_NO_EXISTE`**; un QR ya usado devuelve **409
  `QR_YA_USADO`**, también en reintentos.
- Nueva columna `motivo_invalidacion` en `entradas` (`DEVOLUCION` /
  `CANCELACION`). El estado `EN_REINTEGRO` se conserva para no romper consultas
  ni reportes existentes; la columna solo hace explícito el motivo del rechazo.
- Nuevas columnas de auditoría en `entradas`: `ingresada_en` e
  `id_personal_acceso`, escritas por el mismo `UPDATE` condicional que marca la
  entrada como `USADA`. No se crea una entidad de auditoría separada.
- `UNIQUE` sobre `codigo_qr` en la base de `msvc-entrada`, además del `UPDATE`
  condicional ya existente, para reforzar "un QR nunca ingresa dos veces".
- Tests nuevos del comportamiento de QR con el patrón que ya usa el módulo
  (JUnit + Testcontainers/PostgreSQL), sin modificar `NoDobleVentaTest` ni
  migrar la estrategia a docker-compose en este change.
- Alineación documental: `openspec/config.yaml:20-21` todavía nombra
  `POST /api/entradas/escanear`; la ruta vigente aprobada es
  `POST /api/accesos/escanear` (`gateway-jwt-e2/design.md:289-290`).

## Capacidades

- `qr-acceso`: nueva capacidad de Entrega 2. Validación del QR, motivos de
  rechazo, ingreso único y registro de quién escaneó.

## Cambio respecto a la spec de Entrega 1

La spec de `cancelacion-entradas` (Entrega 1) define que una entrada emitida
pasa a `EN_REINTEGRO` tanto por devolución aprobada como por cancelación del
evento. Ese change todavía no está archivado, pero su comportamiento ya está
implementado y se modifica aquí:

| Aspecto | Entrega 1 | Entrega 2 (este change) |
|---|---|---|
| Estado tras devolución aprobada | `EN_REINTEGRO` | `EN_REINTEGRO` + `motivo_invalidacion = 'DEVOLUCION'` |
| Estado tras cancelación de evento | `EN_REINTEGRO` | `EN_REINTEGRO` + `motivo_invalidacion = 'CANCELACION'` |
| Respuesta al escanear una entrada inválida | mensaje genérico | 409 con `code` que identifica el motivo |
| Registro del ingreso | inexistente | `ingresada_en` + `id_personal_acceso` en la entrada |

Motivo: EP-16 exige rechazar con el motivo y dejar constancia del ingreso, sin
cambiar los estados que ya consumen otros flujos.

## Decisiones aprobadas por el grupo

1. Reintento tras un escaneo exitoso → 409 `QR_YA_USADO`, sin respuesta
   idempotente 200.
2. Auditoría en columnas de `Entrada`, sin entidad `RegistroIngreso`.
3. Motivo en la columna `motivo_invalidacion`, conservando `EN_REINTEGRO`.
4. Tests: se mantiene el patrón existente (Testcontainers en el módulo); no se
   migra a docker-compose en este change y no se modifica el test actual.
5. QR inexistente o alterado → 404 `QR_NO_EXISTE`.

## Alcance

- `msvc-entrada/src/main/java/com/ticketya/entrada/dto/request/`: DTO nuevo.
- `msvc-entrada/src/main/java/com/ticketya/entrada/exception/`: error de QR y
  su handler.
- `msvc-entrada/src/main/java/com/ticketya/entrada/entity/Entrada.java`: columnas
  nuevas y `UNIQUE(codigo_qr)`.
- `msvc-entrada/src/main/java/com/ticketya/entrada/repository/EntradaRepository.java`:
  UPDATE condicional con auditoría y motivo.
- `msvc-entrada/src/main/java/com/ticketya/entrada/service/EntradaService.java`:
  validación, motivos y registro del ingreso.
- `msvc-entrada/src/main/java/com/ticketya/entrada/controller/AccesoController.java`:
  DTO dedicado y uso de `X-User-Id`.
- Tests nuevos en `msvc-entrada/src/test/java/...`.
- Documentación: `openspec/config.yaml` y README de entrada si aplica.

## Fuera del alcance

- Cambiar la autorización: el gateway sigue siendo el único que decide el rol
  (ADR-0001); el servicio no valida roles ni lee `X-User-Role`.
- Firma criptográfica o rotación del QR; se conserva el `UUID` actual.
- Broker/RabbitMQ, eventos de dominio nuevos y notificaciones (EP-14).
- Log centralizado, `correlationId`/MDC y archivos compartidos (EP-20). Este
  change solo emite el dato de auditoría que EP-20 consumirá.
- Caché (EP-19), fila virtual (EP-15), saga/resiliencia (EP-13) y reportes
  (EP-18).
- Frontend o app de escaneo (EP-22).
- Migrar la estrategia de tests a docker-compose ni tocar
  `NoDobleVentaTest` (ADR-0002 queda como está).
- Nuevas dependencias o tecnologías: por eso no se requiere un ADR nuevo.

## Impacto

El personal de acceso recibe respuestas con un `code` estable y puede distinguir
"ya usado" de "cancelado" o "devuelto". Los flujos de compra, devolución y
cancelación no cambian de estados; solo pasan a escribir el motivo. La
autorización y el formato 401/403 del gateway no se modifican.
