# Diseño: verificación criptográfica real del JWT

## Decisión general

Usar el `JwtDecoder` real que construye Spring Boot con las properties
`spring.security.oauth2.resourceserver.jwt.*`, apuntando a una clave pública
generada en el test. Se conservan los 40 tests actuales con decoder falso para
la cadena HTTP; esta clase nueva cubre exclusivamente la criptografía.

## Generación de claves

- `KeyPairGenerator.getInstance("RSA")`, 2048 bits.
- Clave privada en PEM PKCS#8 a un archivo temporal para firmar con
  `NimbusJwtEncoder`.
- Clave pública en PEM X.509 a otro archivo temporal; su ruta se inyecta en
  `JWT_PUBLIC_KEY_LOCATION` con `@DynamicPropertySource`.
- `JWT_ISSUER` y `JWT_AUDIENCE` se fijan al mismo par que usa el emisor según
  ADR-0003.
- `@TempDir` garantiza que no quede nada en el repositorio ni en el workspace.
- No se agrega dependencia nueva: `spring-security-oauth2-jose` ya está
  disponible transitivamente desde el starter de Resource Server. Si hiciera
  falta declararla, sería en `scope test` y no cambia el comportamiento de
  producción.

## Escenarios a ejercitar

| Escenario | Cómo se construye | Resultado esperado |
|---|---|---|
| Firma inválida | token firmado con otra clave privada | 401 uniforme |
| `exp` vencido | `exp` en el pasado | 401 uniforme |
| `iss` distinto | issuer diferente al configurado | 401 uniforme |
| `aud` distinto | audience diferente al configurado | 401 uniforme |
| `alg` distinto de RS256 | token firmado con HS256 o header alterado | 401 uniforme |
| `idUsuario` numérico | claim con valor Long, como emite `TokenServiceImpl` | 200, `X-User-Id` correcto, sin `X-User-Role` |

El escenario «claim `idUsuario` ausente» ya está cubierto por
`GatewayErrorHandlingTest`; no se duplica.

## Modelo de test

`@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc`. Sin
`@MockBean JwtDecoder`, sin `@TestConfiguration` que lo reemplace y sin tokens
“mágicos”. Las peticiones pasan por la cadena real: filtro de identidad,
Resource Server y matriz de roles.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Claves fijas en `src/test/resources` | Sería un secreto versionado, aunque sea de test; ADR-0003 lo prohíbe. |
| Seguir con decoder falso | No ejecuta Nimbus; no prueba nada de lo que queremos verificar. |
| JWKS con `MockWebServer` | Agrega tecnología y configuración nueva; no hace falta para un par estático. |
| Testcontainers | No hay servicio externo que levantar; el decoder es local. |

## Riesgos

- Si el `JwtAuthenticationConverter` actual espera `idUsuario` como String, el
  test con valor numérico lo detectará. La corrección se documenta como defecto
  encontrado, en commit separado, sin relajar la aserción.
- Los tokens de test deben incluir `iat`, `exp`, `iss`, `aud`, `idUsuario` y
  `roles` para respetar ADR-0003. Si falta alguno, el test lo declara
  explícitamente en el nombre del caso.

## ADR

No corresponde uno nuevo: no hay tecnología ni patrón nuevo. Se apoya en
ADR-0001 (el gateway valida) y ADR-0003 (contrato del token).
