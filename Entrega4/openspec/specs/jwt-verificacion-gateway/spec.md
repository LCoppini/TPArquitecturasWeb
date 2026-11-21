# Requisitos: verificación criptográfica real del JWT

## ADDED Requirements

### Requirement: Rechazo por firma inválida [Entrega 2]

El sistema DEBE responder 401 ejecutando el decoder RS256 real cuando la firma
del token no coincide con la clave pública configurada.

#### Scenario: token firmado con otra clave

- Dado que el gateway está configurado con una clave pública RS256 de prueba
- Y el token fue firmado con una clave privada distinta
- Cuando el cliente envía el `Bearer` a una ruta protegida
- Entonces responde 401 con `{status, error, message, path}`
- Y no se propaga ningún `X-User-Id` al servicio destino

### Requirement: Rechazo por token expirado [Entrega 2]

El sistema DEBE responder 401 cuando el claim `exp` del token es anterior al
momento de la solicitud.

#### Scenario: token vencido

- Dado que el token incluye `exp` en el pasado
- Y el resto de los claims son válidos
- Cuando el cliente lo envía a una ruta protegida
- Entonces responde 401 con el formato uniforme de errores
- Y la solicitud no llega al servicio de dominio

### Requirement: Rechazo por issuer distinto [Entrega 2]

El sistema DEBE responder 401 cuando el claim `iss` no coincide con
`JWT_ISSUER`.

#### Scenario: issuer no configurado

- Dado que el token declara un `iss` distinto al configurado en el gateway
- Cuando se valida el token
- Entonces responde 401 con el formato uniforme de errores

### Requirement: Rechazo por audience distinta [Entrega 2]

El sistema DEBE responder 401 cuando el claim `aud` no incluye `JWT_AUDIENCE`.

#### Scenario: audience no configurada

- Dado que el token declara una `aud` distinta a la configurada en el gateway
- Cuando se valida el token
- Entonces responde 401 con el formato uniforme de errores

### Requirement: Rechazo de algoritmos distintos de RS256 [Entrega 2]

El sistema DEBE responder 401 cuando el token no fue firmado con RS256,
incluyendo los algoritmos `none`, HS256, RS384 y RS512.

#### Scenario: algoritmo no permitido

- Dado que el token tiene un header `alg` distinto de `RS256`
- Cuando se valida el token con el decoder real
- Entonces responde 401 con el formato uniforme de errores
- Y no se construye ninguna autoridad `ROLE_*` a partir de ese token

### Requirement: Contrato de identidad con idUsuario numérico [Entrega 2]

El sistema DEBE aceptar `idUsuario` como valor numérico, según ADR-0003, y
DEBE derivar de él el header `X-User-Id` sin propagar `X-User-Role`.

#### Scenario: idUsuario numérico válido

- Dado que el token fue emitido con `idUsuario = 42` como número JSON
- Y contiene `iss`, `aud`, `iat`, `exp` y `roles` válidos
- Cuando se envía a una ruta autorizada para ese rol
- Entonces responde 200
- Y el servicio destino recibe `X-User-Id = 42`
- Y no recibe `X-User-Role`

### Requirement: Evidencia sin secretos versionados [Entrega 2]

El sistema DEBE generar las claves de prueba dentro del test y NO DEBE
versionar claves privadas ni públicas.

#### Scenario: ejecución de la suite de verificación

- Dado que se ejecuta la prueba con el decoder real
- Cuando se generan las claves RS256
- Entonces se escriben solo en un directorio temporal
- Y no aparece ningún archivo `.pem` en el repositorio
