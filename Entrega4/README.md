# TicketYa - Arquitectura Web (TUDAI)

Plataforma de venta de entradas en microservicios (Spring Boot). Esqueleto inicial del proyecto.

## Microservicios

| Modulo | Puerto | Base | Responsabilidad |
|---|---|---|---|
| msvc-config | 8888 | - | Configuracion centralizada (`msvc-config/src/main/resources/configuration/`) |
| msvc-eureka | 8761 | - | Service discovery |
| msvc-gateway | 8080 | - | Punto de entrada unico, ruteo, (E2) JWT RS256 y autorizacion por rol |
| msvc-usuarios | 8081 | ticketya_usuarios | Usuarios y roles |
| msvc-ubicacion | 8082 | ticketya_ubicacion | Lugares y sectores |
| msvc-evento | 8083 | ticketya_evento | Eventos, artistas, precios por sector, busqueda |
| msvc-entrada | 8084 | ticketya_entrada | Reserva, pago (mock), entradas con QR, devoluciones, acceso |
| msvc-notificaciones | 8085 | ticketya_notificaciones | (E2) Envio de emails: compra, cancelacion/cambio de evento, artista favorito |
| msvc-preferencias | 8086 | ticketya_preferencias (MongoDB) | (E2) Preferencias de clientes y recomendaciones |

## Como levantarlo (E1)

1. `docker compose up -d` (PostgreSQL con una base por servicio y MongoDB para preferencias)
2. Arrancar en orden: msvc-config -> msvc-eureka -> msvc-usuarios / ubicacion / evento / entrada / notificaciones / preferencias -> msvc-gateway
3. Swagger de cada servicio: `http://localhost:<puerto>/swagger-ui.html`

### Gateway

Las llamadas externas se realizan por `http://localhost:8080`. El gateway
resuelve los servicios con Eureka y enruta `/api/usuarios` y `/api/roles` a
usuarios; `/api/lugares` y `/api/sectores` a ubicación; `/api/eventos` y
`/api/artistas` a evento; y `/api/entradas`, `/api/pagos` y `/api/accesos` a
entrada. `/api/preferencias` y `/api/recomendaciones` van a preferencias (E2);
notificaciones es de uso interno y no tiene ruta pública.

Las operaciones de devolución están incluidas en `/api/entradas/**`; no hay una
ruta pública para `/api/registros-pago`. El control de acceso usa
`POST /api/accesos/escanear` con `{"idEvento": 10, "codigoQr": "abc"}` y el
`X-User-Id` del JWT; responde 200 con la entrada en estado `USADA`, o un `code`
estable: `QR_REQUEST_INVALIDO`, `QR_NO_EXISTE`, `QR_EVENTO_DISTINTO`,
`QR_DEVUELTO`, `QR_EVENTO_CANCELADO`, `QR_VENCIDO` o `QR_YA_USADO`. La fecha y el
personal que aprobó el ingreso quedan en `ingresada_en` e
`id_personal_acceso`; dentro de `EN_REINTEGRO`, `motivo_invalidacion` distingue
`DEVOLUCION` de `CANCELACION`. La `UNIQUE` de `codigo_qr` no es una migración:
si hay datos previos, ver el script en
`openspec/changes/archive/2026-11-20-control-acceso-qr/design.md`.
En la Entrega 2 el gateway es un
**OAuth2 Resource Server RS256**: valida el JWT, autoriza por rol según
`GatewayRoleMatrix` y deriva `X-User-Id` del claim `idUsuario`. `X-User-Role` no
se propaga. Rutas inexistentes responden 404 y destinos no disponibles 503 con
el formato JSON uniforme del gateway; los rechazos 401/403 usan el mismo
contrato. Los errores de negocio devueltos por un servicio se mantienen sin
modificaciones.

Los clientes consultan `msvc-config` durante el arranque. Sus imports son opcionales para tolerar indisponibilidad temporal, y cada `application.yml` local conserva valores de respaldo para poder iniciar sin el servidor. La configuración central se sirve desde los YAML empaquetados en `msvc-config/src/main/resources/configuration/`.

### Seguridad del gateway (Entrega 2)

- Los clientes envían `Authorization: Bearer <token>` en cada llamada a
  `http://localhost:8080/api/**`. Son públicos `POST /api/usuarios/login`,
  `POST /api/usuarios/registro`, `GET /api/eventos/**` y `GET /api/artistas/**`.
- Variables obligatorias: `JWT_PUBLIC_KEY_LOCATION`, `JWT_AUDIENCE` y
  `JWT_ISSUER` (ver `.env.example`). Sin ellas el gateway **no arranca**
  (fail-closed): nunca vuelve a la identidad simulada de Entrega 1.
- Autorización por path: `/api/entradas/**` y `/api/pagos/**` para `CLIENTE`;
  `/api/admin/**` para `ADMINISTRADOR`; `/api/organizador/eventos/**` para
  `ORGANIZADOR`; `/api/accesos/**` para `PERSONAL_ACCESO`; `/internal/**` queda
  denegado. `/actuator/**` y `/error` están fuera de la cadena de seguridad.
- El token lo emite `msvc-usuarios` en `POST /api/usuarios/login` (ADR-0001 y
  ADR-0003); el gateway solo valida.
- Los rechazos 401/403 usan `{status, error, message, path}`.
- Verificación: `mvn -pl msvc-gateway test` (40 tests, 0 fallos).
  `GatewayRoleMatrixTest` falla si se agrega una ruta al YAML sin regla de rol.

## Arquitectura de cada servicio: capas

Cada microservicio de dominio sigue la **arquitectura en capas**; las dependencias van siempre hacia abajo:

```
Cliente -> controller -> service -> repository -> Base de datos
           (presentacion)  (negocio)   (persistencia)
```

```
src/main/java/com/ticketya/<servicio>/
  controller/             presentacion: recibe HTTP, valida y delega (sin logica de negocio)
  service/ , service/impl/  negocio: reglas, transacciones, llamadas a otros servicios
  repository/ , entity/   persistencia: Spring Data JPA y entidades
  dto/request, dto/response, dto/interservice
  mapper/                 entidad <-> DTO (MapStruct)
  exception/              excepciones de negocio + GlobalExceptionHandler
  client/                 clients Feign hacia otros microservicios
  config/                 configuracion Spring
  event/, messaging/      (E2) eventos y mensajeria asincrona
src/test/java/...         service (unitarios), controller (@WebMvcTest), integration
```

Reglas: el controller solo habla con el service; el service es el unico que usa los repositories; las entidades nunca se exponen en la API (se usan DTOs).
Cada paquete tiene un `package-info.java` que explica que va ahi.

## Metodologia: OpenSpec

OpenSpec ya esta inicializado en este repo (`openspec/` y `.claude/`): NO hace falta repetir `openspec init`.
Cada integrante solo instala la CLI una vez (Node.js 20.19+):

```
npm install -g @fission-ai/openspec@latest
```
Flujo de trabajo (con Claude Code, desde la raiz de Entrega4):
```
/opsx:explore -> /opsx:propose <nombre> -> (revision del grupo) -> /opsx:apply -> /opsx:archive
```
Changes sugeridos: `usuarios-roles`, `lugares-sectores`, `catalogo-eventos`, `busqueda-eventos`, `reserva-asientos`, `compra-idempotente`, `devoluciones`.
Despues de actualizar la CLI: `openspec update`.

## Puntos abiertos del diseno (resolver antes de implementar)

1. **PK de Entrada**: en el diagrama es (id_evento, id_usuario); eso impide comprar 2 entradas del mismo evento. Usar id propio + UNIQUE (evento, sector, asiento).
2. **Precio**: el enunciado lo define por sector *para cada evento* -> `EventoSector` en msvc-evento (no en Sector). Entrada guarda el precio como snapshot.
3. **Sector**: falta tipo numerado / no numerado y asientos.
4. **Reserva temporal y devoluciones**: no figuran en el diagrama; definir si son entidades propias en msvc-entrada.
5. **Evento -> organizador**: se guarda solo `id_organizador`; se valida contra msvc-usuarios (rol ORGANIZADOR).
6. Typos del diagrama: `apellido` figura como INTEGER; `dni` como INTEGER/Long.
7. **Zona horaria al conectar a PostgreSQL:** en el entorno local probado, la JVM usa `America/Buenos_Aires`, que el servidor PostgreSQL rechazó al abrir la conexión (`invalid value for parameter "TimeZone": "America/Buenos_Aires"`). No es un requisito universal de la aplicación: depende de la zona horaria que reporte la JVM y de los nombres aceptados por el servidor, pero puede afectar a cualquier servicio que use el driver PostgreSQL (`msvc-usuarios`, `msvc-ubicacion`, `msvc-evento` y `msvc-entrada`) al ejecutar ambos en esa misma combinación de entorno. Para ese caso, iniciar la JVM con `-Duser.timezone=UTC`; por ejemplo, en IntelliJ agregarlo a **VM options**, al ejecutar un JAR usar `java -Duser.timezone=UTC -jar <jar>`, o con Maven usar `mvn -pl msvc-usuarios -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC" spring-boot:run`. No hace falta modificar `application.yml` ni `docker-compose.yml`; en un entorno donde la JVM ya use una zona aceptada por PostgreSQL no se necesita el flag.
8. **Email del destinatario (E2):** `msvc-notificaciones` necesita el email del cliente, pero `Usuario` hoy no lo tiene (dni, nombre, apellido, ciudad, roles). Definir si se agrega `email` a msvc-usuarios o si el email viaja en el evento/solicitud de notificacion.
9. **Servicios nuevos de la Entrega 2:** solo existe el esqueleto (estructura, pom, configuracion y `package-info.java` con lo previsto). Sin logica; cada integrante la completa con su change de OpenSpec.
