# Requisitos: seguridad del gateway

## ADDED Requirements

### Requirement: Autorización del reporte del organizador [Entrega 2]

El gateway DEBE exigir el rol `ORGANIZADOR` para consultar reportes de eventos
y DEBE propagar la identidad del token sin reenviar el rol del cliente.

#### Scenario: organizador autorizado

- Dado que el JWT contiene `roles = ["ORGANIZADOR"]`
- Cuando solicita `GET /api/organizador/eventos/{idEvento}/reporte`
- Entonces el gateway no responde 401 ni 403
- Y reenvía `X-User-Id` igual a `idUsuario`

#### Scenario: roles no autorizados

- Dado que el JWT contiene `CLIENTE`, `ADMINISTRADOR` o `PERSONAL_ACCESO`
- Cuando solicita el reporte
- Entonces responde 403 con `{status, error, message, path}`

#### Scenario: sobrescritura de identidad y descarte de rol

- Dado que el cliente envía `X-User-Id: 99` y `X-User-Role: ORGANIZADOR`
- Pero su JWT corresponde al usuario 42 con rol `CLIENTE`
- Cuando se enruta cualquier petición protegida
- Entonces el servicio destino recibe `X-User-Id: 42`
- Y no recibe `X-User-Role`
- Y la evidencia de este comportamiento vive en
  `GatewayErrorHandlingTest`, no en `GatewayRoleMatrixTest`

#### Scenario: cobertura de la matriz

- Dado que la ruta del reporte empieza con
  `/api/organizador/eventos/**`
- Cuando se revisa `GatewayRoleMatrix`
- Entonces esa ruta queda cubierta por la regla existente de `ORGANIZADOR`
- Y `GatewayRoleMatrixTest` incluye el path exacto del reporte solo para casos
  de rol permitido y denegado
- Y no se duplican en esa matriz los casos de identidad ya cubiertos por
  `GatewayErrorHandlingTest`

### Requirement: Contratos internos denegados en el gateway [Entrega 2]

El gateway DEBE denegar `/internal/**` con independencia del rol, usando el
mismo formato uniforme de errores.

#### Scenario: contrato interno sin token

- Dado que un cliente llama
  `GET /internal/entradas/eventos/{idEvento}/reporte` sin token
- Cuando la cadena de seguridad evalúa `/internal/**`
- Entonces responde 401 con `{status, error, message, path}`
- Y no enruta la solicitud a `msvc-entrada`

#### Scenario: contrato interno con token

- Dado que un cliente válido con cualquier rol llama al mismo contrato
  interno con token
- Cuando la cadena de seguridad evalúa `/internal/**`
- Entonces responde 403 con `{status, error, message, path}` por `denyAll`
- Y no enruta la solicitud a `msvc-entrada`

#### Scenario: evidencia del comportamiento

- Dado que `SecurityConfig` declara
  `.requestMatchers("/internal/**").denyAll()`
- Cuando se verifican los tests del gateway
- Entonces el caso con token ya está cubierto por
  `GatewayRoleMatrixTest` (`contrato interno siempre denegado`)
- Y el caso sin token se agrega en `GatewayErrorHandlingTest`
