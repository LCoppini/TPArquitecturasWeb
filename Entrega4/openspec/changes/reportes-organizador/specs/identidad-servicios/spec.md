# Requisitos: identidad propagada a los servicios

## ADDED Requirements

### Requirement: Pertenencia aplicada al reporte del organizador [Entrega 2]

El servicio dueño del reporte DEBE validar la pertenencia del recurso usando
únicamente el `X-User-Id` propagado por el gateway.

#### Scenario: pertenencia del reporte

- Dado que el gateway ya autorizó el rol `ORGANIZADOR`
- Cuando `msvc-evento` atiende `GET /api/organizador/eventos/{idEvento}/reporte`
- Entonces compara `evento.getIdOrganizador()` contra `X-User-Id`
- Y responde 403 si no coinciden
- Y no invoca `validarRol` ni `tieneRol` contra `UsuarioClient`

#### Scenario: identidad interservicio

- Dado que `msvc-entrada` expone el contrato interno del reporte
- Cuando `msvc-evento` lo consume por Feign
- Entonces la llamada no lleva JWT de usuario
- Y no depende del rol enviado por el cliente
