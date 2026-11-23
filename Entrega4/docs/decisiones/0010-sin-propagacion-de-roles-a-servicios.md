# ADR-0010: No propagar roles del JWT a los microservicios de dominio

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

La seguridad vigente centraliza validación JWT y autorización por ruta en `msvc-gateway`. El gateway deriva `X-User-Id` del token, pero descarta `X-User-Role`; `msvc-entrada` necesita verificar que el organizador que administra la fila sea dueño del evento.

## Decisión

Solo el organizador propietario administra la fila. Gateway exige rol ORGANIZADOR y `msvc-entrada` compara `X-User-Id` confiable con `idOrganizador` obtenido de `msvc-evento`. No se consulta a Usuarios para el rol, no se agrega endpoint a Evento y no se cambia el contrato de identidad interservicio.

## Alternativa descartada

Propagar roles en un header desde el gateway para permitir también la administración por ADMINISTRADOR. Se descarta porque modifica un componente compartido y los contratos de los servicios para cubrir un actor que este change no requiere.

## Consecuencias

La administración depende de la consulta Feign a Evento y falla cerrada: 404 si el evento no existe, 503 si Evento no está disponible, sin cambiar la fila. Las llamadas Feign siguen sin transportar JWT; es una limitación conocida. La administración por ADMINISTRADOR queda fuera de alcance.

## Referencias

- `docs/decisiones/0001-jwt-resource-server.md`
- `openspec/changes/fila-virtual/specs/fila-virtual/spec.md`
- `openspec/changes/fila-virtual/design.md`
