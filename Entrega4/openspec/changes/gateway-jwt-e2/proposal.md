# Propuesta: seguridad JWT en el gateway (Entrega 2)

## Por qué

En Entrega 1 el gateway reenvía los headers `X-User-Id` y `X-User-Role` tal como
los envía el cliente. Eso permite simular identidad, pero **no autentica ni
autoriza**: cualquier cliente podría enviar el id o el rol de otro usuario.

La épica EP-12 exige reemplazar esa simulación por autenticación real. El
`openspec/config.yaml` ya fija las decisiones del grupo: el login vive en
`msvc-usuarios`, el gateway valida y autoriza, y los servicios de dominio solo
necesitan saber **quién** es el usuario.

## Qué cambia

- El gateway pasa a ser un **OAuth2 Resource Server** con tokens **RS256**. El
  par de claves se genera en `msvc-usuarios`, que emite el JWT en
  `POST /api/usuarios/login` (endpoint público). El gateway solo valida con la
  clave pública configurada por `public-key-location`.
- El token incluye los claims `idUsuario` y `roles`. El gateway mapea `roles` a
  authorities con prefijo `ROLE_` y decide por ruta con
  `requestMatchers(...).hasRole(...)` en una `SecurityFilterChain` de Java.
- El filtro global del gateway **descarta** cualquier `X-User-Id` entrante y lo
  reemplaza por el claim `idUsuario` del JWT. **Se elimina `X-User-Role`**: los
  servicios de dominio dejan de recibirlo y de usarlo.
- Si el token no trae el claim `idUsuario`, el gateway responde **401**; nunca
  propaga `null`.
- Los rechazos 401 y 403 usan el mismo JSON que el resto de errores del gateway:
  `status`, `error`, `message` y `path`.
- La autorización por rol se traslada por completo al gateway. Los servicios
  conservan únicamente las reglas de **pertenencia** (comparar el `X-User-Id`
  recibido contra el dueño del recurso).
- Las pruebas de la cadena de seguridad se implementan con `MockMvc` y un
  `JwtDecoder` de test: **40 tests vigentes**. Las pruebas E2E con docker-compose
  y la prueba de carga con JMeter se trasladan al change `verificacion-e2e-carga`
  (EP-21) y quedan fuera de este alcance.

## Capacidades

- Nueva capacidad: `gateway-seguridad` — validación RS256, matriz rol → path,
  identidad derivada del token, errores 401/403 uniformes.
- Nueva capacidad: `identidad-servicios` — los servicios reciben solo
  `X-User-Id` y no validan roles ni token.

## Cambio respecto a la spec archivada de Entrega 1

La capability `gateway-ruteo` (`openspec/changes/gateway-entrega1/specs/gateway-ruteo/spec.md`)
decía que el gateway propagaba `X-User-Id` y `X-User-Role` **sin autenticar**.
Eso cambia:

| Aspecto | Entrega 1 | Entrega 2 |
|---|---|---|
| `X-User-Id` | se propaga tal cual lo envía el cliente | se descarta el del cliente y se deriva del claim `idUsuario` |
| `X-User-Role` | se propaga | se elimina; la autorización pasa al gateway |
| Acceso a rutas | cualquier cliente | solo con JWT válido y rol habilitado |
| Errores | 404/503 uniformes | se suman 401/403 con el mismo formato |

Motivo: EP-12 y la decisión del grupo de que el gateway sea el único punto de
entrada y autorización. El ruteo por Eureka y los paths públicos **no cambian**.

## Alcance

- `Entrega4/msvc-gateway/pom.xml`: dependencias de seguridad.
- `Entrega4/msvc-config/src/main/resources/configuration/msvc-gateway.yml` y
  `Entrega4/msvc-gateway/src/main/resources/application.yml`: configuración del
  decoder RS256 por variables de entorno.
- Clases nuevas en `com.ticketya.gateway.security` y
  `com.ticketya.gateway.filter`.
- Ajustes en los controllers de `msvc-entrada`, `msvc-evento` y en el
  `EntradaClient` de `msvc-evento` para quitar `X-User-Role`.
- ADRs `0001-jwt-resource-server.md` y `0002-estrategia-pruebas-e2.md`.
- Tests de la cadena de seguridad del gateway (40 vigentes).

## Fuera del alcance

- Generar tokens en el gateway ni exponer `/api/authenticate` allí.
- Agregar base de datos, JPA o entidades de usuario al gateway.
- Usar JJWT, HS256 ni secreto compartido versionado.
- Mantener `X-User-Role` en ningún servicio ni llamada Feign.
- Circuit breaker, caché, fila virtual, notificaciones, trazabilidad
  (correlationId y log de requests), dockerización, docker-compose, JMeter y
  Swagger propio del gateway. La trazabilidad del gateway se propone en el
  change `trazabilidad-gateway`; la dockerización y JMeter en
  `verificacion-e2e-carga` (EP-21).
- Modificar tests existentes sin documentar el motivo. Los reemplazos
  necesarios se hicieron en commits separados y están explicados en `tasks.md`.

## Traslado de tareas a EP-21

Las tareas «pruebas de acceso por rol por endpoint sobre docker-compose» y
«prueba de carga JMeter con al menos 2 instancias de `msvc-entrada` e informe»
se trasladan al change `verificacion-e2e-carga` (EP-21). Por eso el total de
tareas de este change pasa de **60 a 58**: el progreso se mide sobre el nuevo
denominador. No se descarta trabajo; cambia de change y de épica, y se declara
acá para que el número no se interprete como «se quitaron tareas para cerrar».

## Impacto

Los clientes envían `Authorization: Bearer <token>` en cada llamada a
`http://localhost:8080/api/**`, salvo `POST /api/usuarios/login`. Los servicios
siguen leyendo `X-User-Id`, pero ahora es un valor derivado del token.

## Supuestos

- **Los microservicios solo son accesibles a través del gateway.** El bloqueo
  de puertos directos es una decisión de red o de bind en el despliegue, no
  algo que resuelva Spring Security por sí solo.
- Las llamadas entre servicios (Feign) no llevan JWT de usuario y quedan fuera
  de este control; se documenta como limitación conocida.

## Estado

Enmienda para cierre: se declara EP-21 fuera de alcance, se migran sus dos
tareas al change `verificacion-e2e-carga` y se documenta la cobertura real de
tests. Las tareas de implementación del gateway y de alineación de rutas se
cierran según `tasks.md`; este change se archiva cuando estén todas tildadas.

## Decisiones ya tomadas por el grupo

1. RS256 con par de claves; no HS256.
2. Solo `X-User-Id` en los servicios; `X-User-Role` eliminado.
3. Integración con docker-compose; pruebas de rol y prueba de carga con JMeter.
   Se trasladan a `verificacion-e2e-carga` (EP-21), fuera de este change.
4. Separación física de endpoints por rol dentro de `msvc-entrada` y
   `msvc-evento` (ver lista en `design.md`).
5. `GET /api/entradas/disponibilidad` se mueve a
   `GET /api/eventos/{idEvento}/disponibilidad` y queda público dentro de
   `GET /api/eventos/**`; solo devuelve cantidades y asientos libres.
6. Se eliminan `PUT/DELETE /api/entradas/{id}`; se verificó que ningún servicio
   los invoca y las bajas se cubren con devolución y cancelación de evento.
7. Escaneo QR en `POST /api/accesos/escanear` con `PERSONAL_ACCESO`; el gateway
   rutea `/api/accesos/**` a `msvc-entrada`.
8. Clave pública por `public-key-location` / `JWT_PUBLIC_KEY_LOCATION`, sin JWKS.
   Clave privada solo por `JWT_PRIVATE_KEY`, nunca versionada; se entrega
   `.env.example`.
9. Mantener `/api/artistas/**`: `GET` público y alta/edición en
   `/api/admin/artistas/**` con `ADMINISTRADOR`. “Artista” designa a cualquier
   participante de un evento y la entidad conserva `tipo` para distinguirlos.
10. Login con `email` + password BCrypt en `POST /api/usuarios/login`. El registro
    público crea solo `CLIENTE`; los demás roles se cargan por seed o por
    `/api/admin/usuarios` protegido.

## Decisiones pendientes

Ninguna por ahora.
