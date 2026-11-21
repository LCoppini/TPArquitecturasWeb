# Tareas

## Revisión previa

- [x] Revisar proposal, design y specs EARS con el grupo y aprobarlos antes de
      escribir código.
- [x] Confirmar que no se modificarán los 40 tests existentes ni el contrato
      ADR-0003.

## Implementación

- [x] Generar el par RS256 temporal en el test con `@TempDir` y
      `@DynamicPropertySource`.
- [x] Test: firma inválida → 401 con formato uniforme.
- [x] Test: token expirado → 401 con formato uniforme.
- [x] Test: `iss` distinto → 401 con formato uniforme.
- [x] Test: `aud` distinto → 401 con formato uniforme.
- [x] Test: `alg` distinto de RS256 → 401 con formato uniforme.
- [x] Test: `idUsuario` numérico → `X-User-Id` correcto y sin `X-User-Role`.
- [x] Verificar que el decoder usado es el real (sin `@MockBean JwtDecoder`).

## Verificación y cierre

- [x] Ejecutar `mvn -pl msvc-gateway test` y `mvn -B package` completos.
- [x] Publicar `jwt-verificacion-gateway` en `openspec/specs/`.
- [x] No agregar ADR: no hay tecnología ni patrón nuevo.
- [x] Archivar el change con `.openspec.yaml` → `archived: <fecha>`.
