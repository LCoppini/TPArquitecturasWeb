# Propuesta: verificación criptográfica real del JWT en el gateway (Entrega 2)

## Por qué

El change `gateway-jwt-e2` dejó 40 tests verdes, pero todos inyectan un
`JwtDecoder` de prueba que devuelve un `Jwt` construido a mano. El header
`alg: RS256` se escribe en esos tokens, pero **no se valida**: el decoder falso
no ejecuta Nimbus. No hay escenarios automatizados de firma inválida,
expiración, `iss`, `aud` ni algoritmo distinto de RS256.

La configuración de producción es correcta (clave pública por
`JWT_PUBLIC_KEY_LOCATION`, `issuer` y `audience` obligatorios) y el smoke test
documentado con claves temporales fue anterior al refactor. EP-12 exige que un
token ausente, vencido o inválido devuelva 401; hoy eso está resuelto por
configuración, pero no por evidencia de test.

## Qué cambia

- Se agrega una clase de test con un par RS256 **generado dentro del test** y el
  `JwtDecoder` real del Resource Server.
- Se cubren los rechazos 401 por: firma inválida, `exp` vencido, `iss` distinto,
  `aud` distinto y `alg` distinto de RS256.
- Se verifica el contrato de ADR-0003 con el emisor real: `idUsuario` numérico
  llega como `X-User-Id` y `X-User-Role` no se propaga.
- No se modifica código de producción salvo que el test descubra un defecto; en
  ese caso se documenta y se corrige en commit separado.
- No se modifica ningún test existente: los 40 actuales siguen cubriendo la
  cadena HTTP con decoder falso. Este change solo **agrega** evidencia.

## Capacidades

- Nueva capacidad: `jwt-verificacion-gateway` — evidencia automatizada de la
  validación criptográfica del JWT en el gateway.
- Impacto en `gateway-seguridad`: complementa el requisito «Validación de JWT
  RS256 en el gateway» con escenarios verificables; no cambia su comportamiento.

## Alcance

- `msvc-gateway/src/test/java/.../JwtVerificacionRealTest.java` (o el nombre que
  acuerde el grupo).
- Generación de claves en `@TempDir` y properties inyectadas con
  `@DynamicPropertySource`.
- Documentación del change y publicación de la spec viva al archivar.

## Fuera del alcance

- docker-compose, Dockerfiles, JMeter y pruebas E2E (EP-21).
- Cambiar ADR-0001, ADR-0003 ni el contrato del token.
- Emitir tokens en producción ni versionar claves privadas o públicas.
- Trazabilidad/correlationId y log de requests (change `trazabilidad-gateway`,
  Fase 2, queda para después).
- Modificar tests existentes para que pasen.

## Impacto

Ningún cambio en el comportamiento del gateway. Un token inválido ya era
rechazado por la configuración real; a partir de este change ese rechazo queda
demostrado por tests ejecutables en CI, sin depender de claves ni servicios
externos.

## Estado

Archivado el 2026-11-20 con 14/14 tareas. El test con decoder real detectó un
defecto en la validación de `iss`, corregido en commit `2677cc7` y documentado
en `design.md`. Los 40 tests existentes no se modificaron; la suite nueva
agrega 11 escenarios (gateway: 51/51 = 40 existentes + 11 nuevos) y el build
completo de los 10 módulos pasa. La spec viva queda en `openspec/specs/jwt-verificacion-gateway` y
complementa `openspec/specs/gateway-seguridad`. EP-21 y la Fase 2
(`trazabilidad-gateway`) siguen fuera de alcance.
