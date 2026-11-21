# Tareas

## Revisión previa

- [ ] Revisar proposal, design y specs EARS con el grupo y aprobarlos antes de
      escribir código.
- [ ] Confirmar que no se modificarán los 40 tests existentes ni el contrato
      ADR-0003.

## Implementación

- [ ] Generar el par RS256 temporal en el test con `@TempDir` y
      `@DynamicPropertySource`.
- [ ] Test: firma inválida → 401 con formato uniforme.
- [ ] Test: token expirado → 401 con formato uniforme.
- [ ] Test: `iss` distinto → 401 con formato uniforme.
- [ ] Test: `aud` distinto → 401 con formato uniforme.
- [ ] Test: `alg` distinto de RS256 → 401 con formato uniforme.
- [ ] Test: `idUsuario` numérico → `X-User-Id` correcto y sin `X-User-Role`.
- [ ] Verificar que el decoder usado es el real (sin `@MockBean JwtDecoder`).

## Verificación y cierre

- [ ] Ejecutar `mvn -pl msvc-gateway test` y `mvn -B package` completos.
- [ ] Publicar `jwt-verificacion-gateway` en `openspec/specs/`.
- [ ] No agregar ADR: no hay tecnología ni patrón nuevo.
- [ ] Archivar el change con `.openspec.yaml` → `archived: <fecha>`.
