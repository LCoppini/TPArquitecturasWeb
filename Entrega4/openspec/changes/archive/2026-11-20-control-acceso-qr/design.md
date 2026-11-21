# Diseño: control de acceso con QR (Entrega 2)

## Decisión general

Se conserva el mecanismo ya aprobado y existente: **actualización condicional
del estado** en la base de `msvc-entrada`, no `@Version` ni bloqueos nuevos. La
consigna EP-16 admite "UPDATE condicional o `@Version`" y el `UPDATE` actual ya
resuelve la concurrencia a nivel de base de datos, incluso con varias
instancias del servicio.

No se agrega ninguna tecnología ni patrón nuevo. Por eso **no corresponde un
ADR nuevo**: ADR-0001 (autorización en el gateway, sin `X-User-Role`) y ADR-0002
(estrategia de pruebas) siguen vigentes y no se modifican.

```
Personal de acceso
      |  POST /api/accesos/escanear  { idEvento, codigoQr }
      v
msvc-gateway  --- hasRole(PERSONAL_ACCESO), X-User-Id del JWT --->  msvc-entrada
                                                                      |
                                                   AccesoController (DTO propio)
                                                                      |
                                                   EntradaService.validarQr
                                                                      |
                                     UPDATE entradas SET estado='USADA',
                                       ingresada_en=now(),
                                       id_personal_acceso=:id
                                     WHERE codigo_qr=:qr
                                       AND id_evento=:idEvento
                                       AND estado='VALIDA'
                                                                      |
                                                   filas afectadas == 1 ?
                                                      |            |
                                                     sí           no
                                                   200 USADA    409 QrError
```

## Contrato del endpoint

### Request

`QrEscaneoRequest` (DTO nuevo, solo para este endpoint):

| Campo | Tipo | Validación |
|---|---|---|
| `idEvento` | `Long` | `@NotNull` |
| `codigoQr` | `String` | `@NotBlank` |

Se deja de usar `EntradaRequest`: exigía `idSector` y permitía enviar
`idUsuario`, `precio`, `fila`, `asiento`, `claveIdempotencia` y
`aprobarDevolucion`, campos que no intervienen en el ingreso.

La identidad del personal llega únicamente por el header `X-User-Id`, que el
gateway deriva del claim `idUsuario` del JWT (ADR-0001). El controller deja de
ignorarlo y lo pasa al service para la auditoría. El servicio **no** valida el
rol ni lee `X-User-Role`.

### Response

Se conserva `EntradaResponse` para el caso exitoso (estado `USADA`) para no
romper clientes. El `code` de error viaja solo en las respuestas de rechazo.

### Catálogo de errores

| Situación | HTTP | `code` | Origen |
|---|---|---|---|
| QR vacío o request inválido | 400 | `QR_REQUEST_INVALIDO` | validación Bean Validation |
| QR inexistente o alterado | 404 | `QR_NO_EXISTE` | `findByCodigoQr` vacío |
| QR de otro evento | 409 | `QR_EVENTO_DISTINTO` | comparación `idEvento` |
| Entrada devuelta | 409 | `QR_DEVUELTO` | `motivo_invalidacion = 'DEVOLUCION'` |
| Evento cancelado | 409 | `QR_EVENTO_CANCELADO` | `motivo_invalidacion = 'CANCELACION'` |
| Reserva vencida | 409 | `QR_VENCIDO` | estado `VENCIDA`/`CANCELADA` |
| Ya ingresada o perdió la carrera | 409 | `QR_YA_USADO` | `marcarUsada != 1` o estado `USADA` |

`QrRechazadoException` lleva el `QrError`; un `@ExceptionHandler` dedicado
escribe `{"error": mensaje, "code": code}`. No se modifica el manejo genérico de
`EntityNotFoundException`/`IllegalStateException` de otros endpoints.

Decisión aprobada: un reintento posterior a un ingreso exitoso devuelve 409
`QR_YA_USADO`. No se devuelve 200 idempotente, para que el doble ingreso sea
siempre visible como rechazo.

## Modelo de datos

Se agregan tres columnas a `entradas` en `msvc-entrada`:

| Columna | Tipo | Uso |
|---|---|---|
| `motivo_invalidacion` | `varchar(24)`, nullable | `DEVOLUCION` o `CANCELACION` cuando el estado es `EN_REINTEGRO` |
| `ingresada_en` | `timestamp`, nullable | fecha del ingreso aprobado |
| `id_personal_acceso` | `bigint`, nullable | `X-User-Id` que aprobó el ingreso |

`codigo_qr` pasa a ser `unique = true`. En PostgreSQL, las filas con
`codigo_qr` nulo (reservas aún no pagadas) no colisionan entre sí, así que la
restricción no requiere cambiar el flujo de reserva.

El estado `EN_REINTEGRO` **no se parte** en dos estados. La columna
`motivo_invalidacion` es suficiente para responder el motivo y evita tocar las
consultas que ya filtran por `EN_REINTEGRO`.

Punto de cuidado: el proyecto usa `ddl-auto: update` (sin Flyway/Liquibase), que
no es una migración: crea columnas/índices nuevos pero **no sanea datos previos**.
La restricción `UNIQUE` y las columnas nuevas se validan con la BD real del test;
si la tabla ya tiene duplicados en `codigo_qr`, Hibernate no podrá crear el
índice y el arranque fallará. No se agrega una herramienta de migraciones en este
change; antes de desplegar hay que ejecutar, con la aplicación detenida y con
revisión de quién conservar, este script:

```sql
-- 1) Debe devolver 0 filas. Si devuelve datos, no desplegar sin decidir cuál conservar.
SELECT codigo_qr, count(*)
FROM entradas
WHERE codigo_qr IS NOT NULL
GROUP BY codigo_qr
HAVING count(*) > 1;

-- 2) Solo después de revisar el caso: conservar la entrada más antigua.
-- DELETE FROM entradas e
-- USING entradas d
-- WHERE e.codigo_qr = d.codigo_qr
--   AND e.codigo_qr IS NOT NULL
--   AND e.id > d.id;

-- 3) Aplicar la restricción (si ddl-auto: update no lo hizo).
ALTER TABLE entradas ADD CONSTRAINT uk_entrada_codigo_qr UNIQUE (codigo_qr);
```

Las filas con `codigo_qr` nulo (reservas sin pagar) no se agrupan en el paso 1 y
no colisionan con la restricción; hay un test de integración que lo verifica.

## Flujo del service

1. Validar request (`@Valid`).
2. Buscar la entrada por `codigoQr`. Si no existe → 404 `QR_NO_EXISTE`.
3. Comparar `idEvento`. Si difiere → 409 `QR_EVENTO_DISTINTO`.
4. Evaluar estado:
   - `USADA` → 409 `QR_YA_USADO`.
   - `EN_REINTEGRO` → 409 con el `code` según `motivo_invalidacion`.
   - `VENCIDA`/`CANCELADA` → 409 `QR_VENCIDO`.
   - `VALIDA` → continuar.
5. Ejecutar el `UPDATE` condicional, que además graba `ingresada_en` e
   `id_personal_acceso`. Si devuelve 0 filas → 409 `QR_YA_USADO` (otro escaneo
   ganó la carrera).
6. Devolver la respuesta con estado `USADA`. Para evitar devolver una entidad
   con estado viejo del contexto de JPA, se recarga la entrada o se construye el
   DTO con los valores ya validados + `ingresada_en`.

No se usa `findByCodigoQr` con bloqueo pesimista: el `UPDATE` condicional ya
serializa el cambio de estado y funciona con varias instancias.

## Concurrencia y multiinstancia

Dos escaneos simultáneos pueden leer ambos `VALIDA`. Solo uno logra que el
`UPDATE` afecte una fila; el otro recibe 0 filas y responde 409 `QR_YA_USADO`.
Esto no depende de memoria local ni del número de réplicas.

La UNIQUE sobre `codigo_qr` evita además que dos entradas distintas compartan
el mismo código por un error de generación/almacenamiento.

## Auditoría y datos sensibles

El registro queda en la propia entrada (`ingresada_en`,
`id_personal_acceso`). Se emite además un log estructurado de operación crítica
con `idEntrada`, `idEvento`, `idPersonal`, resultado y `code`.

El QR **no se escribe en los logs**: es una credencial de ingreso. EP-20
(correlationId, MDC y archivos compartidos) consumirá estos datos; este change
no implementa esa infraestructura.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| `@Version` optimista sobre `Entrada` | Agrega conflictos a otros flujos (pago, devolución, cancelación); el UPDATE condicional ya resuelve el caso con menos impacto. |
| Entidad `RegistroIngreso` | Permitiría auditar rechazos, pero amplía el alcance; el grupo decidió mantener la auditoría en `Entrada`. |
| Estados nuevos `DEVUELTA`/`CANCELADA_PARA_REINTEGRO` | Rompería consultas existentes; se prefiere la columna `motivo_invalidacion`. |
| Respuesta 200 idempotente en el reintento | El grupo decidió 409 `QR_YA_USADO` para que el doble ingreso sea explícitamente un rechazo. |
| QR firmado/JWT por entrada | No lo pide la consigna; el UUID actual más UNIQUE y UPDATE condicional cubre el requisito. |
| Bloqueo pesimista en la lectura | Innecesario y más costoso; la decisión atómica se toma en el UPDATE. |
| Tests con docker-compose | ADR-0002 lo prevé para integración entre servicios, pero el grupo decidió mantener por ahora el patrón Testcontainers ya usado en el módulo. |

## Pruebas

- Tests nuevos de `EntradaService.validarQr`: éxito, QR inexistente, otro
  evento, devolución, cancelación, vencida, ya usada y reintento.
- Test de concurrencia con PostgreSQL real para dos escaneos del mismo QR:
  exactamente uno 200 y el otro 409, con una sola fila `USADA` y un solo
  `id_personal_acceso` grabado.
- Test de controller: DTO rechaza `codigoQr` en blanco y `idEvento` nulo; el
  `X-User-Id` llega al service.
- Test de la matriz del gateway ya existente cubre 403 para CLIENTE y acceso
  para PERSONAL_ACCESO; se verificará que siga verde sin tocarlo.
- No se modifica `NoDobleVentaTest` ni se migran los tests a docker-compose en
  este change.

## Riesgos y pendientes

- `ddl-auto: update` no es una migración: validar la UNIQUE contra la BD real y
  documentar el script si hay datos previos.
- `config.yaml:20-21` y el README pueden tener la ruta vieja
  `/api/entradas/escanear`; alinear a `/api/accesos/escanear` sin pisar cambios
  de otras personas.
- La devolución y la cancelación se escriben en varios puntos del service y del
  repository; todos deben cargar `motivo_invalidacion` para que el motivo sea
  correcto.
- No archivar el change hasta que las specs y el README estén alineados.
