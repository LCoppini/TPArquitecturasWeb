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
  clave pública/JWKS correspondiente.
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
- Las pruebas de integración se planifican con **docker-compose**, no con
  Testcontainers. Se agregan pruebas de acceso por rol y una prueba de carga con
  JMeter y su informe de resultados. En este paso **no se escribe ningún test**.

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
- Estructura de pruebas documentada (sin código de test en este paso).

## Fuera del alcance

- Generar tokens en el gateway ni exponer `/api/authenticate` allí.
- Agregar base de datos, JPA o entidades de usuario al gateway.
- Usar JJWT, HS256 ni secreto compartido versionado.
- Mantener `X-User-Role` en ningún servicio ni llamada Feign.
- Implementar el login en `msvc-usuarios` (requiere su propio change: email,
  password BCrypt, emisión RS256 y JWKS).
- Definir la separación final de paths por rol dentro de `msvc-entrada` y
  `msvc-evento` (pendiente de decisión del grupo; ver `design.md`).
- Circuit breaker, caché, fila virtual, notificaciones, trazabilidad,
  dockerización y Swagger agregado en el gateway.
- Escribir o ejecutar tests en este paso.

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

Estructura propuesta para revisión del grupo. Las tareas de implementación están
sin tildar. No se agregó código de producción ni de test en este paso.

## Decisiones ya tomadas por el grupo

1. RS256 con par de claves; no HS256.
2. Solo `X-User-Id` en los servicios; `X-User-Role` eliminado.
3. Integración con docker-compose; pruebas de rol y prueba de carga con JMeter.

## Decisiones pendientes

1. Publicar JWKS desde `msvc-usuarios` vs entregar la clave pública por archivo
   montado (`public-key-location`).
2. Separación física de endpoints por rol dentro de `msvc-entrada` y
   `msvc-evento` (ver lista en `design.md`).
3. Alinear `/api/artistas/**` con el controlador real `/api/participantes` y la
   ruta de escaneo de QR (`/api/entradas/accesos/validar` en código vs
   `/api/entradas/escanear` en el config).
4. Definir el atributo de password de `Usuario` para el login (punto abierto del
   config).
