# Tareas

## Revisión previa

- [x] Confirmar que el alcance es solo EP-16 y que el gateway ya está resuelto
      en `gateway-jwt-e2` (ADR-0001).
- [x] Decisión 1: reintento tras ingreso exitoso → 409 `QR_YA_USADO`, sin
      respuesta 200 idempotente.
- [x] Decisión 2: auditoría en columnas de `Entrada`, sin entidad nueva.
- [x] Decisión 3: motivo en columna `motivo_invalidacion`, conservando
      `EN_REINTEGRO`.
- [x] Decisión 4: tests con el patrón existente (Testcontainers); no se migra a
      docker-compose ni se modifica `NoDobleVentaTest`.
- [x] Decisión 5: QR inexistente o alterado → 404 `QR_NO_EXISTE`.
- [x] Revisar proposal, design y specs EARS con el grupo y aprobarlos antes de
      escribir código.

## Contrato y errores

- [x] Crear `QrEscaneoRequest` con `idEvento` (`@NotNull`) y `codigoQr`
      (`@NotBlank`).
- [x] Cambiar `AccesoController` para usar el DTO nuevo y pasar `X-User-Id` al
      service.
- [x] Crear `QrError` y `QrRechazadoException` con los códigos previstos en el
      diseño.
- [x] Agregar el `@ExceptionHandler` que devuelve `{"error", "code"}` sin
      modificar el handler genérico de otros endpoints. Quedó acotado a
      `AccesoController` (`@RestControllerAdvice(assignableTypes = ...)`).
- [x] Test de controller: DTO inválido, propagación de `X-User-Id` y mapeo de
      errores.

## Motivo de invalidación

- [x] Agregar `motivo_invalidacion` a `Entrada` y escribirlo en la devolución
      aprobada (`DEVOLUCION`).
- [x] Escribir `motivo_invalidacion = 'CANCELACION'` en la cancelación de evento
      (`cancelarReservasDeEvento` / `marcarEntradasDeEventoEnReintegro`).
- [x] Test: devolución y cancelación conservan `EN_REINTEGRO` pero difieren en
      el motivo (unitario de devolución e integración de cancelación).
- [x] Documentar en la propuesta/spec el cambio respecto a la spec de Entrega 1
      de `cancelacion-entradas` (verificado: sigue vigente en `proposal.md`).

## Ingreso único y auditoría

- [x] Agregar `ingresada_en` e `id_personal_acceso` a `Entrada`.
- [x] Extender `marcarUsada` para grabar fecha y personal en el mismo `UPDATE`
      condicional.
- [x] Reescribir `validarQr` con el orden de validación del `design.md` y los
      códigos de error aprobados.
- [x] Evitar devolver un DTO con estado viejo del contexto de JPA: se construye
      con el estado nuevo y los valores del `UPDATE`.
- [x] Emitir el log de operación crítica sin el `codigoQr` completo.
- [x] Tests de service: éxito, QR inexistente, otro evento, devolución,
      cancelación, vencida, ya usada y reintento.

## Unicidad y base de datos

- [x] Declarar `UNIQUE` sobre `codigo_qr` en `Entrada`.
- [x] Verificar contra PostgreSQL real que la restricción no afecta a reservas
      sin QR y que un QR duplicado se rechaza.
- [x] Documentar que `ddl-auto: update` no es una migración y qué script haría
      falta si hubiera datos duplicados (ver `design.md`).

## Concurrencia

- [x] Test nuevo con PostgreSQL real: dos escaneos simultáneos del mismo QR,
      uno 200 y uno 409, una sola fila `USADA` y un solo registro de personal.
- [x] No tocar `NoDobleVentaTest` ni la estrategia de Tests existente.
- [x] Verificar que `GatewayRoleMatrixTest` siga verde sin cambios (build
      completo verde).

## Documentación y cierre

- [x] Alinear `openspec/config.yaml:20-21` de `/api/entradas/escanear` a
      `/api/accesos/escanear`, coordinando con quien esté tocando el archivo.
- [x] Actualizar el README (no hay README propio de `msvc-entrada`): ruta,
      códigos de error y columnas de auditoría.
- [x] Verificar `mvn -pl msvc-entrada test` y `mvn -B package` completos.
- [x] No agregar ADR: no hay tecnología ni patrón nuevo en este change.
- [x] Al final: mover la spec a `openspec/specs/`, archivar el change y marcar
      la fecha de archivado (2026-11-20).
