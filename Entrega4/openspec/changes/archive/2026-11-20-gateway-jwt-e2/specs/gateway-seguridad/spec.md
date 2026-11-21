# Requisitos: seguridad del gateway

## ADDED Requirements

### Requirement: Validacion de JWT RS256 en el gateway [Entrega 2]

Cuando una solicitud alcance una ruta protegida bajo `/api/**`, el gateway DEBE
validar el token Bearer con el algoritmo RS256 antes de reenviarla.

#### Scenario: solicitud sin token

- Dado que el cliente llama una ruta protegida
- Cuando no envía el header `Authorization`
- Entonces el gateway responde 401
- Y el body usa el formato uniforme con `status`, `error`, `message` y `path`

#### Scenario: token con firma invalida, vencido o de otra audiencia

- Dado que el cliente envía `Authorization: Bearer <token>`
- Cuando el token fue firmado con otra clave, está vencido o su `aud` no coincide
- Entonces el gateway responde 401
- Y no reenvía la solicitud al servicio destino

#### Scenario: login y registro publicos

- Dado que un cliente no autenticado llama `POST /api/usuarios/login` o
  `POST /api/usuarios/registro`
- Cuando el gateway evalúa la cadena de seguridad
- Entonces la solicitud se permite sin token y se enruta a `msvc-usuarios`
- Y cualquier otra ruta bajo `/api/**` sigue exigiendo autenticación

#### Scenario: consultas publicas

- Dado que un cliente no autenticado llama `GET /api/eventos/**` o
  `GET /api/artistas/**`
- Cuando el gateway evalúa la cadena de seguridad
- Entonces la solicitud se permite sin token
- Y las operaciones de escritura sobre artistas exigen `ADMINISTRADOR` en
  `/api/admin/artistas/**`

### Requirement: Claim idUsuario obligatorio [Entrega 2]

El gateway DEBE obtener la identidad del claim `idUsuario` del JWT y DEBE
rechazar cualquier token que no lo contenga.

#### Scenario: identidad derivada del claim

- Dado que el JWT contiene `idUsuario` igual a `42` y `roles` igual a `["CLIENTE"]`
- Cuando el gateway autentica la solicitud
- Entonces el principal se deriva de `idUsuario`, no del claim `sub`

#### Scenario: token sin el claim idUsuario

- Dado que el cliente envía un JWT con firma válida pero sin `idUsuario`
- Cuando el gateway intenta propagar la identidad
- Entonces responde 401 con el formato uniforme
- Y no reenvía `X-User-Id` con valor nulo ni vacío al servicio destino

#### Scenario: el cliente intenta spoofear la identidad

- Dado que el cliente envía `X-User-Id: 99` y `X-User-Role: ADMINISTRADOR`
- Pero su JWT corresponde al usuario `42` con rol `CLIENTE`
- Cuando el gateway reenvía la solicitud
- Entonces el servicio recibe `X-User-Id: 42`
- Y no recibe `X-User-Role`

### Requirement: Autorizacion por matriz rol/path [Entrega 2]

El gateway DEBE mapear el claim `roles` a authorities con prefijo `ROLE_` y
DEBE aplicar `requestMatchers(...).hasRole(...)` por ruta antes de enrutar.

#### Scenario: rol permitido

- Dado que el JWT contiene un rol autorizado para el path solicitado
- Cuando el gateway evalúa la matriz
- Entonces reenvía la solicitud al servicio destino

#### Scenario: rol no autorizado

- Dado que el JWT es válido pero su rol no figura en la matriz del path
- Cuando el gateway evalúa la matriz
- Entonces responde 403
- Y el body usa el formato uniforme del gateway

#### Scenario: la matriz cubre todas las rutas publicas

- Dado que existen rutas públicas configuradas en el gateway
- Cuando se revisa la matriz de seguridad
- Entonces `POST /api/usuarios/login`, `POST /api/usuarios/registro`,
  `GET /api/eventos/**` y `GET /api/artistas/**` están declaradas como
  `permitAll` antes de la matriz
- Y el resto de las rutas tiene una regla explícita o autenticación genérica

### Requirement: Errores 401 y 403 uniformes [Entrega 2]

Cuando el gateway rechace una solicitud por autenticación o autorización, DEBE
responder con la misma estructura JSON usada para 404 y 503.

#### Scenario: cuerpo del error de seguridad

- Dado que el gateway rechaza una solicitud con 401 o 403
- Cuando el cliente lee el body
- Entonces contiene `status`, `error`, `message` y `path`
- Y no expone stack traces, tokens ni detalles internos

### Requirement: Configuracion RS256 sin secretos versionados [Entrega 2]

La configuración JWT del gateway DEBE usar `public-key-location` con la clave
pública y la audiencia desde variables de entorno, sin JWKS ni valores sensibles
en el repositorio.

#### Scenario: no hay secretos en el repositorio

- Dado que se revisan los YAML del gateway y de configuración central
- Cuando se busca la configuración de `resourceserver.jwt`
- Entonces los valores se leen de `${JWT_PUBLIC_KEY_LOCATION}` y
  `${JWT_AUDIENCE}`
- Y no aparece `jwk-set-uri`, clave privada ni secreto literal

#### Scenario: claves por entorno

- Dado que `msvc-usuarios` firma el token con `JWT_PRIVATE_KEY`
- Cuando se revisa `.env.example`
- Entonces las variables JWT tienen valores de ejemplo y no claves reales
- Y `.env` no está versionado

#### Scenario: configuracion incompleta

- Dado que no se definen las variables de entorno JWT
- Cuando arranca el gateway
- Entonces el contexto falla en lugar de arrancar sin validación
- Y no se habilita el modo simulado de Entrega 1

### Requirement: Acceso externo solo por el gateway [Entrega 2]

Los servicios de dominio NO DEBEN estar expuestos directamente a clientes
externos; el acceso a las APIs públicas DEBE pasar por `msvc-gateway`.

#### Scenario: llamada directa a un puerto de servicio

- Dado que un cliente intenta llamar `http://localhost:8081/api/usuarios/...`
- Cuando no atraviesa el gateway
- Entonces la solicitud es rechazada por la configuración de red o de enlace
  documentada en el ADR
- Y el cliente debe usar `http://localhost:8080/api/usuarios/...` con su JWT

### Requirement: Esquema Bearer en la documentacion [Entrega 2]

Los contratos OpenAPI de las APIs públicas DEBEN declarar el esquema de
seguridad Bearer JWT.

#### Scenario: documentacion del esquema

- Dado que se genera la documentación OpenAPI de un servicio de dominio
- Cuando se consulta la sección de seguridad
- Entonces aparece un esquema `bearerAuth` de tipo HTTP Bearer JWT

> La verificación de acceso por rol sobre `docker-compose` y la prueba de carga
> con JMeter se trasladan al change `verificacion-e2e-carga` (EP-21). No forman
> parte de los requisitos de este change; aquí solo se conserva la matriz
> automatizada con `MockMvc` descrita en `GatewayRoleMatrixTest`.
