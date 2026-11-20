# ADR-0003: Contrato del token JWT de TicketYa

- Estado: aceptada
- Fecha: 2026-11-20

## Contexto

ADR-0001 decidió que `msvc-usuarios` emite el JWT y que `msvc-gateway` solo lo
valida como Resource Server RS256. Para que emisor y validador no divergan, hay
que fijar por escrito el algoritmo, los claims, los nombres, la expiración y el
formato de las claves antes de implementar el login.

También hay que evitar dos problemas concretos:

- que un claim se llame de una forma en `msvc-usuarios` y de otra en el gateway;
- que la clave privada termine versionada, en un YAML o en un `Dockerfile`.

## Decisión

El token emitido por `msvc-usuarios` y validado por `msvc-gateway` tendrá este
contrato, de cumplimiento obligatorio en ambos servicios.

### Algoritmo y header

| Elemento | Valor |
|---|---|
| `alg` | `RS256` |
| `typ` | `JWT` |
| `kid` | No se emite: existe una sola clave activa; la rotación requiere despliegue coordinado |

El gateway rechaza cualquier token cuyo header no indique `RS256`. No se acepta
`none`, HS256 ni otro algoritmo.

### Claims

| Claim | Tipo | Valor / regla |
|---|---|---|
| `iss` | string | `ticketya`, leído de `JWT_ISSUER` |
| `aud` | string o array de strings | `ticketya`, leído de `JWT_AUDIENCE`; debe coincidir con la audiencia configurada en el gateway |
| `exp` | número epoch en segundos | `iat + 900` (15 minutos), configurado con `JWT_EXPIRATION_SECONDS` |
| `iat` | número epoch en segundos | instante de emisión |
| `idUsuario` | número entero positivo | `Usuario.idUsuario`; se transporta como JSON number |
| `roles` | array de strings | nombres exactos de los roles del usuario |

Los únicos valores válidos dentro de `roles` son:

```text
CLIENTE
ORGANIZADOR
ADMINISTRADOR
PERSONAL_ACCESO
```

El claim se llama **`roles` en ambos servicios**. No se usan `role`,
`authorities`, `permisos` ni variantes en singular. El prefijo `ROLE_` **no** se
incluye en el token: `JwtAuthenticationConverter` lo agrega en el gateway para
que `hasRole("CLIENTE")` funcione.

Ejemplo de payload:

```json
{
  "iss": "ticketya",
  "aud": "ticketya",
  "iat": 1763640000,
  "exp": 1763640900,
  "idUsuario": 42,
  "roles": ["CLIENTE", "ORGANIZADOR"]
}
```

### Claims prohibidos

El token **no** incluye:

- `sub`: la identidad se toma de `idUsuario` con `setPrincipalClaimName`.
- `password`, hash, email, DNI ni otros datos personales.
- `X-User-Role` ni ningún header: los headers son transporte, no claims.
- secretos ni claves.

### Expiración y reloj

- Validez: **900 segundos (15 minutos)** por defecto.
- `exp` es obligatorio. El gateway rechaza tokens vencidos con **401**.
- No se configura skew adicional; emisor y validador usan reloj del sistema.

### Validación en el gateway

El gateway valida, en este orden:

1. firma RS256 con la clave pública configurada;
2. `iss` = `JWT_ISSUER`;
3. `aud` contiene `JWT_AUDIENCE`;
4. `exp` vigente;
5. presencia de `idUsuario`;
6. authorities derivadas de `roles` con prefijo `ROLE_`.

Si falla 1 a 5, responde **401** con `GatewayErrorResponse`. Si la firma y los
claims son válidos pero el rol no alcanza para la ruta, responde **403**. Nunca
reenvía un token inválido ni propaga un `X-User-Id` vacío.

### Claves

El par se genera con OpenSSL, una sola vez por entorno:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt_private.pem
openssl rsa -in jwt_private.pem -pubout -out jwt_public.pem
```

- `jwt_private.pem` debe ser **PKCS#8**: empieza con `-----BEGIN PRIVATE KEY-----`.
  No se acepta PKCS#1 (`-----BEGIN RSA PRIVATE KEY-----`).
- `jwt_public.pem` es SubjectPublicKeyInfo: empieza con
  `-----BEGIN PUBLIC KEY-----`.
- La privada se entrega **únicamente** por la variable de entorno
  `JWT_PRIVATE_KEY`, como PEM de una línea con `\n` escapados. Ningún proceso la
  lee desde un archivo del repositorio.
- La pública se entrega al gateway por `JWT_PUBLIC_KEY_LOCATION`
  (`file:/run/secrets/ticketya_jwt_public.pem` o `classpath:`).
- No se publica JWKS, no hay endpoint `/.well-known/jwks.json` y no se usa
  `kid` para seleccionar claves.
- `.env.example` contiene solo placeholders; `.env` y los `.pem` reales no se
  versionan.

### Variables de entorno

| Variable | Consumida por | Uso |
|---|---|---|
| `JWT_PRIVATE_KEY` | `msvc-usuarios` | firma PKCS#8 |
| `JWT_PUBLIC_KEY_LOCATION` | `msvc-gateway` | verificación |
| `JWT_ISSUER` | ambos | valor de `iss` y su validación |
| `JWT_AUDIENCE` | ambos | valor de `aud` y su validación |
| `JWT_EXPIRATION_SECONDS` | `msvc-usuarios` | duración del token, por defecto `900` |

### Dependencias

- `msvc-usuarios`: `spring-security-crypto` para `BCryptPasswordEncoder` y
  `spring-security-oauth2-jose` para `NimbusJwtEncoder`. **No** se agrega
  `spring-boot-starter-security`: eso activaría filtros que cerrarían los
  endpoints del servicio; el control de acceso lo hace el gateway.
- `msvc-gateway`: `spring-boot-starter-oauth2-resource-server`, ya presente.
- No se usa JJWT ni un secreto HS256.

## Alternativas consideradas

1. **`sub` como identidad.** Descartada: el proyecto ya define `idUsuario` como
   claim de identidad y el gateway usa `setPrincipalClaimName`.
2. **`role` en singular.** Descartada: un usuario puede tener varios roles y el
   `config.yaml` habla de `roles`.
3. **Incluir `ROLE_` en el token.** Descartada: acoplaría el formato del token a
   la convención de Spring y duplicaría el prefijo al converter.
4. **HS256 con secreto compartido.** Descartada en ADR-0001: RS256 separa firma
   y verificación.
5. **JWKS con `kid`.** Descartada en ADR-0001: agrega un endpoint público y una
   dependencia innecesaria para una única clave.
6. **Clave privada en archivo del repo o en YAML.** Descartada: viola
   `AGENTS.md` y el principio de secretos por entorno.
7. **Expiración de 24 horas o sin `exp`.** Descartada: aumenta la ventana de
   exposición y contradice el contrato de Resource Server.

## Consecuencias

### Mejoras

- Emisor y validador comparten un único contrato verificable.
- El claim de roles es estable y no depende de convenciones de cada servicio.
- La clave privada nunca entra al repositorio ni al gateway.
- Los servicios reciben solo `X-User-Id`, derivado de un claim entero.

### Costos y riesgos

- Rotar la clave requiere generar el par, actualizar `JWT_PRIVATE_KEY` y
  `JWT_PUBLIC_KEY_LOCATION` y reiniciar ambos servicios de forma coordinada.
- Un token emitido antes de un cambio de `iss`/`aud` deja de ser válido.
- Hay que mantener `JWT_EXPIRATION_SECONDS` igual en la configuración de
  emisión; el gateway solo valida `exp`, no la duración.
- La prueba de matriz debe usar tokens con este mismo contrato para no validar
  un escenario distinto del real.

## Referencias

- `docs/decisiones/0001-jwt-resource-server.md`
- `openspec/config.yaml`, sección "Seguridad (Entrega 2)"
- `openspec/changes/gateway-jwt-e2/design.md`
- `.env.example`
