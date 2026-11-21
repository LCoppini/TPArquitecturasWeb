# Requisitos: control de acceso con QR

## ADDED Requirements

### Requirement: Solicitud de escaneo dedicada [Entrega 2]

El sistema DEBE aceptar el escaneo en `POST /api/accesos/escanear` con un DTO
propio que contenga `idEvento` y `codigoQr`, y DEBE rechazar los campos
obligatorios vacíos antes de consultar la base.

#### Scenario: escaneo con datos válidos

- Dado que el personal de acceso envía `{"idEvento": 10, "codigoQr": "abc"}`
- Y el header `X-User-Id` corresponde al personal autenticado por el gateway
- Cuando el sistema valida la solicitud
- Entonces continúa con la validación del QR
- Y el `X-User-Id` se conserva para la auditoría del ingreso

#### Scenario: código QR en blanco

- Dado que el personal envía `codigoQr` vacío o solo espacios
- Cuando el sistema valida la solicitud
- Entonces responde 400 con `code = QR_REQUEST_INVALIDO`
- Y no consulta la base de entradas

#### Scenario: idEvento ausente

- Dado que el personal omite `idEvento`
- Cuando el sistema valida la solicitud
- Entonces responde 400 con `code = QR_REQUEST_INVALIDO`

### Requirement: Validación del QR y motivo del rechazo [Entrega 2]

El sistema DEBE verificar que la entrada exista, que corresponda al evento
indicado y que esté vigente, y DEBE responder con un `code` que identifique el
motivo cuando el rechazo ocurra.

#### Scenario: QR inexistente o alterado

- Dado que no hay ninguna entrada con ese `codigoQr`
- Cuando el sistema busca la entrada
- Entonces responde 404 con `code = QR_NO_EXISTE`
- Y el mensaje no revela si otro QR similar existe

#### Scenario: QR de otro evento

- Dado que la entrada encontrada tiene `idEvento` distinto del recibido
- Cuando el sistema compara los identificadores
- Entonces responde 409 con `code = QR_EVENTO_DISTINTO`
- Y no cambia el estado de la entrada

#### Scenario: entrada devuelta

- Dado que la entrada tiene estado `EN_REINTEGRO` y
  `motivo_invalidacion = 'DEVOLUCION'`
- Cuando se escanea su QR
- Entonces responde 409 con `code = QR_DEVUELTO`

#### Scenario: evento cancelado

- Dado que la entrada tiene estado `EN_REINTEGRO` y
  `motivo_invalidacion = 'CANCELACION'`
- Cuando se escanea su QR
- Entonces responde 409 con `code = QR_EVENTO_CANCELADO`

#### Scenario: reserva vencida o cancelada

- Dado que la entrada tiene estado `VENCIDA` o `CANCELADA`
- Cuando se escanea su QR
- Entonces responde 409 con `code = QR_VENCIDO`

### Requirement: Ingreso único y atómico [Entrega 2]

El sistema DEBE marcar la entrada como `USADA` con una actualización condicional
que solo afecte entradas en estado `VALIDA`, y DEBE rechazar cualquier intento
posterior o concurrente con el mismo QR.

#### Scenario: ingreso aprobado

- Dado que la entrada está en estado `VALIDA` y pertenece al evento indicado
- Cuando el sistema ejecuta la actualización condicional
- Entonces la actualización afecta exactamente una fila
- Y la respuesta es 200 con la entrada en estado `USADA`

#### Scenario: doble escaneo concurrente

- Dado que dos empleados escanean el mismo QR al mismo tiempo
- Y ambas lecturas ven inicialmente el estado `VALIDA`
- Cuando ambas actualizaciones condicionales se ejecutan sobre PostgreSQL real
- Entonces exactamente una devuelve 200 y la otra 409 con `code = QR_YA_USADO`
- Y solo una fila queda en estado `USADA`

#### Scenario: reintento después de un ingreso exitoso

- Dado que el QR ya fue marcado como `USADA`
- Cuando se escanea nuevamente, incluso con otro `X-User-Id`
- Entonces responde 409 con `code = QR_YA_USADO`
- Y no se modifica la fecha ni el personal ya registrado

### Requirement: Registro del ingreso en la entrada [Entrega 2]

El sistema DEBE grabar, en la misma actualización atómica que aprueba el
ingreso, la fecha del ingreso y el `X-User-Id` del personal que lo aprobó.

#### Scenario: auditoría del ingreso aprobado

- Dado que un escaneo es aprobado
- Cuando se ejecuta el `UPDATE` condicional
- Entonces `ingresada_en` queda con la fecha del ingreso
- Y `id_personal_acceso` queda con el `X-User-Id` recibido

#### Scenario: no se registran ingresos rechazados

- Dado que un escaneo es rechazado por cualquier motivo
- Cuando se evalúa el resultado
- Entonces `ingresada_en` e `id_personal_acceso` permanecen sin cambios

#### Scenario: el QR no se expone en logs

- Dado que se registra la operación crítica de ingreso
- Cuando el sistema escribe el log
- Entonces incluye `idEntrada`, `idEvento`, `idPersonal`, resultado y `code`
- Y no incluye el `codigoQr` completo ni datos personales del comprador

### Requirement: Unicidad del código QR [Entrega 2]

El sistema DEBE garantizar en la base de `msvc-entrada` que un `codigoQr` no
pueda repetirse entre entradas, además de la actualización condicional.

#### Scenario: código QR duplicado

- Dado que ya existe una entrada con `codigoQr = "abc"`
- Cuando se intenta persistir otra entrada con el mismo código
- Entonces la base de datos rechaza la escritura por la restricción UNIQUE

#### Scenario: entradas sin QR

- Dado que una reserva todavía no tiene `codigoQr`
- Cuando se persisten varias reservas sin código
- Entonces la restricción UNIQUE no las considera duplicadas

### Requirement: Autorización del endpoint [Entrega 2]

El gateway DEBE permitir `POST /api/accesos/escanear` solo a usuarios con rol
`PERSONAL_ACCESO`, y el servicio de dominio NO DEBE validar roles por sí mismo.

#### Scenario: personal de acceso autorizado

- Dado que el JWT contiene el rol `PERSONAL_ACCESO`
- Cuando se llama `POST /api/accesos/escanear`
- Entonces el gateway reenvía la solicitud a `msvc-entrada`

#### Scenario: otros roles rechazados

- Dado que el JWT contiene `CLIENTE`, `ORGANIZADOR` o `ADMINISTRADOR`
- Cuando se llama `POST /api/accesos/escanear`
- Entonces el gateway responde 403
- Y la solicitud no llega al servicio de dominio

#### Scenario: sin rol de acceso en el servicio

- Dado que `msvc-entrada` recibe `X-User-Id` del gateway
- Cuando procesa el escaneo
- Entonces no exige ni lee `X-User-Role`
- Y solo usa el identificador para la auditoría del ingreso
