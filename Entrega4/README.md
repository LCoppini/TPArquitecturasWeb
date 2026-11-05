# TicketYa - Arquitectura Web (TUDAI)

Plataforma de venta de entradas en microservicios (Spring Boot). Esqueleto inicial del proyecto.

## Microservicios

| Modulo | Puerto | Base | Responsabilidad |
|---|---|---|---|
| msvc-config | 8888 | - | Configuracion centralizada (`msvc-config/src/main/resources/configuration/`) |
| msvc-eureka | 8761 | - | Service discovery |
| msvc-gateway | 8080 | - | Punto de entrada unico, ruteo, (E2) JWT y trazabilidad |
| msvc-usuarios | 8081 | ticketya_usuarios | Usuarios y roles |
| msvc-ubicacion | 8082 | ticketya_ubicacion | Lugares y sectores |
| msvc-evento | 8083 | ticketya_evento | Eventos, artistas, precios por sector, busqueda |
| msvc-entrada | 8084 | ticketya_entrada | Reserva, pago (mock), entradas con QR, devoluciones, acceso |

## Como levantarlo (E1)

1. `docker compose up -d` (PostgreSQL con una base por servicio)
2. Arrancar en orden: msvc-config -> msvc-eureka -> msvc-usuarios / ubicacion / evento / entrada -> msvc-gateway
3. Swagger de cada servicio: `http://localhost:<puerto>/swagger-ui.html`

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
