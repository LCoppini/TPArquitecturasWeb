# Microservicio Evento — implementación de Entrega 1

Este documento registra las tareas realizadas, las decisiones de diseño y el estado de las historias de usuario que involucran a `msvc-evento`. El alcance es únicamente la primera entrega. No se preparan JWT, notificaciones, fila virtual, acceso QR, reportes, recomendaciones, caché ni despliegue integral.

## Contexto y límites del servicio

`msvc-evento` es dueño del catálogo y del ciclo de vida de eventos, además de la relación entre eventos y participantes. Persiste solamente datos propios y referencias por ID a otros servicios:

- `Evento` conserva `idLugar` e `idOrganizador`; no replica las entidades Lugar ni Usuario.
- `Participante` se comparte entre eventos y se relaciona mediante la entidad asociativa `EventoParticipante`, persistida en `evento_participante`. Su atributo `tipo` es texto libre, no un enum ni una lista cerrada.
- `EventoSector` relaciona un evento con `idSector` (referencia por ID, sin FK entre servicios) y el precio acordado para esa combinación. La estructura física del sector y su capacidad pertenecen a Ubicación.
- Entrada es dueña de disponibilidad, reservas, compras y reintegros. Evento expone sus datos por REST y solicita a Entrada la coordinación de reintegros cuando un administrador cancela un evento.

El precio pertenece a la combinación **evento + sector**, no al lugar ni al sector físico. Esto permite que un mismo sector tenga precios distintos para eventos distintos.

## Tareas de implementación

1. **Inspección de estructura y contratos existentes — completada.** Se respetaron las capas y paquetes existentes, Spring Boot, PostgreSQL, Feign y Eureka. No se fijó ningún puerto. Usuario y Ubicación tenían paquetes vacíos; se agregaron las entidades y los endpoints mínimos de consulta que Evento consume. Entrada ya dispone de un endpoint de cancelación.
2. **Modelo JPA y persistencia — completada.** Se agregaron `Evento`, `Participante`, `EventoParticipante`, `EventoSector` y sus claves compuestas. El estado y el tipo de participante se persisten como `String` (sin enums para esos atributos). Las entidades de otros servicios se referencian solo mediante IDs. Al borrar un evento se eliminan sus asociaciones, no participantes ni sectores de otros servicios.
3. **DTOs y mapeo — completada.** Los controladores reciben y devuelven DTOs, nunca exponen entidades JPA. Se agregaron requests validados, respuestas para eventos/participantes y una respuesta paginada.
4. **Clientes Feign — implementados para Usuario, Lugar y Sector.** Sector se consulta por ID para verificar que existe y pertenece al lugar del evento. Usuario y Lugar se consultan para autorización y ciudad. Entrada tiene la ruta de cancelación observada en el código; se requiere añadirle el endpoint de ventas para completar el orden por relevancia.
5. **Reglas de negocio — completada en `msvc-evento`.** Se implementaron creación en borrador, edición de borradores por su organizador, configuración de sectores y precios, publicación validada, suspensión, cancelación administrativa, reintento de coordinación de una cancelación, estados agotado/finalizado y borrado físico restringido a borradores.
6. **Búsqueda — implementada, pendiente del contrato de ventas de Entrada.** Los filtros se combinan, el resultado se ordena antes de paginar, la página por defecto contiene hasta 10 eventos y el máximo admitido es 100. Ciudad se consulta a Ubicación; el orden por precio usa `EventoSector`; Entrada debe proporcionar el total vendido para relevancia.
7. **REST, errores y configuración — completada.** Se agregaron controladores REST, códigos de error uniformes, Swagger por `springdoc` ya presente en el módulo y se conserva la configuración existente de PostgreSQL vía Config Server. No se agregaron puertos ni credenciales al código.
8. **Pruebas unitarias de lógica de negocio — completadas.** Se agregaron 22 pruebas JUnit 5 con Mockito para los servicios de Evento y Participante. Los repositorios y clientes Feign se simulan, por lo que las pruebas no requieren base de datos ni microservicios levantados.

## Paquetes y DTOs

Siguiendo el patrón de `msvc-entrada`, cada interfaz Feign está en `client` y declara el servicio, ruta y método HTTP. Feign deserializa directamente el JSON recibido en DTOs simples dentro de `dto/response` (`UsuarioResponse`, `LugarResponse`, `SectorResponse` y `VentasEventoResponse`). Estos DTOs no son entidades JPA ni se persisten. `dto/request` representa los cuerpos aceptados por la API pública de Evento; `dto/response` contiene tanto respuestas públicas propias como las respuestas Feign.

Todos los estados se representan como `String` en la entidad y en las reglas de negocio. No se utilizan enums; los valores admitidos se guardan en mayúsculas (`BORRADOR`, `PUBLICADO`, `AGOTADO`, `SUSPENDIDO`, `CANCELADO`, `FINALIZADO`).

## Decisiones de comportamiento

- **Identidad simulada:** los endpoints mutables reciben `X-User-Id`. Cada operación consulta `GET /api/usuarios/{id}` por Feign y comprueba el rol devuelto; el ID del organizador se toma del usuario validado, no del cuerpo JSON. No se confía en un rol recibido por header y no se usa JWT en Entrega 1.
- **Edición:** el organizador dueño puede editar el contenido de su evento solamente mientras está en `BORRADOR`. Un administrador no altera el contenido mediante esta API.
- **Sectores y precios:** el organizador dueño configura una lista no vacía de pares `idSector`/`precio` mientras el evento está en `BORRADOR`. Se comprueba por Feign que cada sector exista y que su `idLugar` coincida con el lugar del evento. No se admiten IDs repetidos ni precios negativos. Si al editar el borrador se cambia el lugar, se eliminan sus asociaciones de sectores para que deban configurarse de nuevo.
- **Publicación:** el dueño puede publicar un borrador cuando el lugar existe y hay sectores configurados con precio. Se revalida que cada sector siga existiendo y corresponda al lugar. También puede volver a publicar un evento suspendido, tras repetir las validaciones.
- **Estados:** se usan `BORRADOR`, `PUBLICADO`, `AGOTADO`, `SUSPENDIDO`, `CANCELADO` y `FINALIZADO`. Suspender requiere administrador y estado publicado/agotado. Cancelar requiere administrador para coincidir con la autorización del endpoint de Entrada existente. Agotado solo se establece desde publicado; finalizado requiere fecha pasada y un estado publicado, agotado o suspendido. La transición a agotado/finalizado está reservada a un administrador.
- **Cancelación entre servicios:** se persiste `CANCELADO` y luego se invoca Entrada. Si Entrada falla, el API devuelve `502` explícito, pero no revierte el estado local; el administrador puede reintentar con `POST /api/eventos/{id}/cancelacion/reintentar`. Esta coordinación síncrona no es una transacción distribuida. La notificación a compradores queda fuera de Entrega 1.
- **Borrado físico:** `DELETE` elimina únicamente eventos en borrador, por su organizador o por un administrador. No borra participantes compartidos. Los demás estados conservan su historial y deben cancelarse, no borrarse.
- **Precio de búsqueda:** cuando se pide ordenar por precio, el precio representativo del evento es el menor precio configurado en sus `EventoSector`; el ordenamiento es ascendente o descendente según el criterio. No se hacen llamadas a Ubicación para conocer precios.
- **Relevancia:** se ordena por la cantidad de entradas vendidas, descendente, y se usa fecha ascendente como desempate. Evento no estima las ventas a partir de disponibilidad, porque esta también puede incluir reservas temporales.
- **Búsqueda por defecto:** sin filtros devuelve eventos publicados futuros, del más próximo al más lejano. Con filtros, todos se combinan como AND. La paginación se aplica después del filtrado remoto y del ordenamiento.
- **Participantes:** al crear/editar se puede referenciar un participante existente por `id`, o enviar `nombre` y `tipo` para reutilizarlo si existe o crearlo si es nuevo. `tipo` acepta cualquier texto no vacío de hasta 80 caracteres. `GET /api/participantes` y `GET /api/participantes/{id}` permiten consultarlos.

## API pública de Evento

Todas las rutas usan el prefijo `/api`.

| Método y ruta | Función | Acceso simulado |
|---|---|---|
| `POST /eventos` | Crear evento en borrador; `201 Created` | `X-User-Id` con rol `ORGANIZADOR` |
| `GET /eventos/{id}` | Obtener detalle para otros microservicios; incluye IDs, estado y participantes | Sin autenticación |
| `PUT /eventos/{id}` | Editar evento en borrador | Organizador dueño |
| `PUT /eventos/{id}/sectores` | Reemplazar configuración de sectores y precios del borrador | Organizador dueño |
| `GET /eventos/{id}/sectores/{idSector}` | Consultar precio del sector configurado para ese evento | Sin autenticación |
| `POST /eventos/{id}/publicacion` | Publicar o reanudar evento suspendido, validando Lugar/Sectores | Organizador dueño |
| `POST /eventos/{id}/suspension` | Suspender evento publicado/agotado | Administrador |
| `POST /eventos/{id}/cancelacion` | Cancelar y solicitar el procesamiento a Entrada | Administrador |
| `POST /eventos/{id}/cancelacion/reintentar` | Reintentar la coordinación con Entrada | Administrador |
| `PUT /eventos/{id}/estado` | Establecer `AGOTADO` o `FINALIZADO`, respetando transiciones | Administrador |
| `DELETE /eventos/{id}` | Borrar físicamente un borrador | Organizador dueño o administrador |
| `GET /eventos` | Buscar, ordenar y paginar | Sin autenticación |
| `GET /participantes` | Listar participantes | Sin autenticación |
| `GET /participantes/{id}` | Obtener participante | Sin autenticación |

Parámetros de búsqueda opcionales: `tipo`, `idOrganizador`, `participante` (ID o parte del nombre), `ciudad`, `desde`, `hasta`, `orden`, `pagina` y `tamano`. Los órdenes admitidos son `fecha`, `precioAsc`/`precioDesc` y `relevancia`. Fechas en ISO-8601 UTC, por ejemplo `2030-01-01T00:00:00Z`.

Ejemplo de creación:

```json
{
  "idLugar": 15,
  "nombre": "Festival de ejemplo",
  "descripcion": "Festival al aire libre",
  "tipo": "Recital",
  "fechaHora": "2030-01-01T20:00:00Z",
  "participantes": [
    {"id": 8},
    {"nombre": "Banda de ejemplo", "tipo": "Banda"},
    {"nombre": "Invitado", "tipo": "Artista invitado"}
  ]
}
```

Configurar los sectores y sus precios del evento:

```json
{
  "sectores": [
    {"idSector": 31, "precio": 12500.00},
    {"idSector": 32, "precio": 8500.00}
  ]
}
```

La respuesta de `GET /eventos/{id}` contiene el detalle del evento, `participantes` (cada uno con `id`, `nombre` y `tipo`) y `sectores` como una lista de `{ "idSector": ..., "precio": ... }`. La ruta de precio por sector devuelve `{ "idSector": 31, "precio": 12500.00 }`; Entrada podrá consultarla con el par de IDs del evento y sector. La respuesta de búsqueda tiene `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. Los errores usan `{fecha, estado, error, mensaje, ruta}`; entradas inválidas responden `400`, ausencia `404`, regla de negocio `409`, autorización `403` y caída/contrato inválido de otro servicio `502`.

El cambio de `Artista` a `Participante` modifica el esquema (`artista`/`evento_artista` a `participante`/`evento_participante`). `ddl-auto: update` no garantiza migrar los datos ni las relaciones existentes; si se conserva una base previa, se debe realizar una migración antes de usarla.

### Inciso para probar después: encabezado `X-User-Id`

La autorización de Entrega 1 se simula enviando el ID de un usuario existente en Usuario mediante el encabezado HTTP `X-User-Id`. Evento consulta ese usuario por Feign y verifica sus roles; no se envía un rol en el request ni se usa JWT.

Ejemplos con `curl` (reemplazar los IDs y la URL por los del entorno cuando esté levantado):

```bash
# Crear un borrador como organizador.
curl -X POST "http://localhost:<puerto>/api/eventos" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: <id-organizador>" \
  -d '{
    "idLugar": 15,
    "nombre": "Festival de ejemplo",
    "descripcion": "Festival al aire libre",
    "tipo": "Recital",
    "fechaHora": "2030-01-01T20:00:00Z",
    "participantes": []
  }'

# Publicar o editar un evento propio como el organizador dueño.
curl -X POST "http://localhost:<puerto>/api/eventos/<id-evento>/publicacion" \
  -H "X-User-Id: <id-organizador>"

# Cancelar como administrador.
curl -X POST "http://localhost:<puerto>/api/eventos/<id-evento>/cancelacion" \
  -H "X-User-Id: <id-administrador>"
```

Para comprobar la autorización, repetir una edición/publicación con el ID de otro organizador: se espera `403 Forbidden`. Usar un ID inexistente debe devolver `404 Not Found`; si Usuario no está disponible, se espera un error de dependencia y no una autorización concedida.

## Contratos Feign usados o requeridos

Feign resuelve nombres de servicio por Eureka. No hay URLs ni puertos hardcodeados en los clientes.

| Servicio | Contrato esperado | Estado en el repositorio |
|---|---|---|
| Usuario | `GET /api/usuarios/{id}` devuelve `idUsuario`, nombre, apellido y `roles` como lista de textos. | Implementado como consulta JPA con carga de roles y respuesta DTO. No se agregaron operaciones de alta/edición ni seeds de datos. |
| Ubicación | `GET /api/lugares/{id}` devuelve `id`, nombre, dirección, `ciudad` y `ubicacion`. | Implementado con repositorio, servicio, DTO y controlador de consulta. |
| Ubicación/Sector | `GET /api/sectores/{id}` devuelve `idSector`, `idLugar` y datos estructurales del sector; no incluye precio. | Consumido por Evento para verificar existencia y pertenencia al lugar al configurar y publicar. El contrato de consulta por ID debe estar disponible en Ubicación. |
| Entrada | `POST /api/entradas/eventos/{idEvento}/cancelacion` con headers `X-User-Id` y `X-User-Role: ADMINISTRADOR`. | La ruta existe en Entrada y exige rol administrador por header. |
| Entrada | `GET /api/entradas/eventos/{idEvento}/ventas` con `{ "idEvento": 1, "entradasVendidas": 42 }`. | La ruta no existe actualmente; debe agregarse en Entrada para habilitar el orden exacto por relevancia. El endpoint de disponibilidad existente no distingue reservas de ventas y no se usa como sustituto. |

Las llamadas externas no aplican fallback silencioso: un error de dependencia se propaga como `502`. Para ejecutar el flujo real, las tablas deben tener los usuarios, roles, lugares y sectores correspondientes. Entrada todavía debe exponer el contador de ventas para habilitar relevancia; no se modificó ese microservicio porque el alcance de este cambio era Evento.

## Estado de User Stories vinculadas a Evento

- [x] **US-1 — Crear evento en borrador:** implementada con validación del rol vía Usuario, asociación del organizador y estado inicial `BORRADOR`.
- [x] **US-2 — Configurar sectores y precios:** implementada en Evento como pares `idSector`/`precio`; se valida por Feign que los sectores pertenezcan al lugar del evento.
- [x] **US-3 — Publicar evento:** implementada con validación de lugar y sectores configurados con precio antes de cambiar a `PUBLICADO`. Requiere que Ubicación implemente el GET de sector por ID.
- [ ] **US-4 — Cancelar cualquier evento:** el cambio a `CANCELADO` y la invocación a Entrada están implementados. El reintegro depende del contrato existente de Entrada. Notificar compradores es Entrega 2 y no se implementa.
- [ ] **US-5 — Elegir evento para comprar:** Evento expone detalle con estado, fecha, lugar y participantes. Reservas por 10 minutos, disponibilidad, elección de asiento y límite por cliente corresponden a Entrada.
- [ ] **US-9 — Buscar eventos con filtros:** filtros, combinación AND, fecha, ordenamiento, página por defecto y paginación están implementados. Ciudad se consulta a Ubicación; el precio mínimo se calcula desde `EventoSector`; la relevancia exacta depende de la ruta de ventas de Entrada.
- [ ] **US-10 — Proceder al pago:** Evento expone sus datos por `GET /api/eventos/{id}`; reserva, pago, idempotencia y emisión QR son de Entrada/Pago.
- [ ] **US-6 y US-8:** solicitudes y resolución de devoluciones son responsabilidad de Entrada/Pago.
- [ ] **US-7, US-11, US-12 y US-13:** acceso QR, notificaciones, reportes y fila virtual no se implementan en esta entrega en Evento.

## Verificación

Las pruebas unitarias se alojan en `msvc-evento/src/test/java/com/ticketya/evento/service`:

- `EventoServiceImplTest` (19 casos): creación como borrador y rol del organizador; reutilización de participantes por nombre y tipo; autorización y edición solo en borrador; eliminación de sectores y precios cuando cambia el lugar; rechazo de sectores repetidos o pertenecientes a otro lugar; publicación con sectores y rechazo sin ellos; suspensión administrativa y validación de fecha para finalizar; cancelación con Entrada, conservación del estado cancelado si falla la coordinación y reintento de la coordinación; borrado físico solo de borradores; búsqueda por participante, orden por precio mínimo, paginación y rango de fechas.
- `ParticipanteServiceImplTest` (3 casos): consulta por ID con tipo libre, respuesta de no encontrado y listado con mapeo DTO.

Verificación ejecutada desde `Entrega4` con `..\Entrega3\mvnw.cmd -pl msvc-evento -am test`: **22 pruebas, 0 fallas, 0 errores y 0 omitidas**. Estas pruebas son unitarias; no verifican integración real con PostgreSQL, Eureka ni los otros microservicios. La búsqueda por relevancia también requiere que Entrada implemente el contador de ventas documentado.
