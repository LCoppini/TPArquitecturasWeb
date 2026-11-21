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

#### Scenario: el cliente intenta suplantar al organizador

- Dado que el cliente envía `X-User-Id: 99` y `X-User-Role: ORGANIZADOR`
- Pero su JWT corresponde al usuario 42
- Cuando se enruta el reporte
- Entonces `msvc-evento` recibe `X-User-Id: 42`
- Y no recibe `X-User-Role`

#### Scenario: cobertura de la matriz

- Dado que la ruta del reporte empieza con
  `/api/organizador/eventos/**`
- Cuando se revisa `GatewayRoleMatrix`
- Entonces esa ruta queda cubierta por la regla existente de `ORGANIZADOR`
- Y `GatewayRoleMatrixTest` incluye el path exacto del reporte
