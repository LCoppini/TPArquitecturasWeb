# TicketYa: Épicas del proyecto

Arquitectura Web (TUDAI) · Trabajo práctico grupal 2026 · Entrega 1 y Entrega 2

## Convenciones

- **Épica:** objetivo de negocio que se entrega a través de varios changes de OpenSpec. Las historias (US-xx) son las del documento de user stories.
- **Servicios:** los de la estructura del proyecto. `msvc-entrada` concentra reserva, pago (mock), entradas, devoluciones y acceso. Los servicios que aparecen en la entrega 2 se marcan como nuevos.
- **Responsables:** propuesta para cinco personas. P1 entrada (núcleo de compra), P2 entrada (post-compra), P3 evento, P4 usuarios y ubicación, P5 plataforma.
- **Tamaño:** esfuerzo relativo (S, M, L, XL), no horas.
- **Definición de hecho común:** código con sus tests (nadie modifica un test para que pase), spec de OpenSpec revisada por el grupo antes de aplicarla, endpoints documentados en OpenAPI, requests en la colección de Postman, README actualizado y, si se suma una tecnología, un ADR en `docs/decisiones`.

## Mapa de épicas

| ID | Épica | Entrega | Servicio | Resp. | Historias | Tamaño |
| --- | --- | --- | --- | --- | --- | --- |
| EP-01 | Plataforma base y gateway | E1 | config, eureka, gateway | P5 | Transversal | L |
| EP-02 | Usuarios y roles | E1 | usuarios | P4 | US-34 (parte E1) | S |
| EP-03 | Lugares y sectores | E1 | ubicacion | P4 | US-01 | M |
| EP-04 | Eventos, precios y publicación | E1 | evento | P3 | US-02 a US-06 | L |
| EP-05 | Búsqueda y detalle de eventos | E1 | evento | P3 | US-07 a US-09 | M |
| EP-06 | Disponibilidad y reserva temporal | E1 | entrada | P1 | US-10, 11, 14, 15 | XL |
| EP-07 | No doble venta e idempotencia | E1 | entrada | P1 | US-17 a US-19 | L |
| EP-08 | Pago y emisión de entradas | E1 | entrada | P2 | US-12, 13, 16 | L |
| EP-09 | Devoluciones y cancelación de eventos | E1 | entrada, evento | P2 y P3 | US-20 a US-22 | L |
| EP-10 | Contratos, documentación y datos de prueba | E1 | todos | P5 con todos | Transversal | M |
| EP-11 | Estrategia de testing de la entrega 1 | E1 | todos | Cada dueño, P5 revisa | Transversal | M |
| EP-12 | Seguridad con JWT y roles | E2 | gateway y todos | P5 y P4 | US-34 | L |
| EP-13 | Consistencia entre servicios y resiliencia | E2 | entrada y clientes Feign | P1 y P2 | US-36 | XL |
| EP-14 | Mensajería y notificaciones | E2 | notificaciones (nuevo), broker | P4 | US-30 a US-32 | L |
| EP-15 | Fila virtual | E2 | fila (nuevo, Redis) | P1 | US-23 a US-25 | L |
| EP-16 | Control de acceso con QR | E2 | entrada | P2 | US-26, 27 | M |
| EP-17 | Preferencias y recomendaciones | E2 | recomendaciones (nuevo, MongoDB) | P3 | US-28, 29 | L |
| EP-18 | Reportes para organizadores | E2 | reportes (nuevo) | P4 | US-33 | M |
| EP-19 | Cache | E2 | evento, gateway | P3 | US-37 | M |
| EP-20 | Trazabilidad y log centralizado | E2 | todos | P5 | US-35 | M |
| EP-21 | Despliegue y prueba de carga | E2 | todos | P5 | US-38 | L |
| EP-22 | Interfaz gráfica (opcional) | E2 | frontend | A definir | Transversal | L |

**Orden de ejecución sugerido (E1):** EP-01, EP-02 y EP-03 arrancan primero porque los demás dependen de sus contratos. EP-04 y EP-06 corren en paralelo, con `entrada` simulando a `evento` hasta que esté listo. EP-07 se diseña junto con EP-06, no después. EP-08 sigue a EP-06, y EP-09 a EP-08. EP-05 entra cuando EP-04 está estable. EP-10 y EP-11 son continuas.

## Entrega 1: núcleo del sistema

### EP-01 · Plataforma base y gateway

**Entrega 1 · msvc-config, msvc-eureka, msvc-gateway · P5 · Tamaño L**

**Objetivo.** Dejar un esqueleto ejecutable sobre el que el resto integra sin pisarse: configuración, descubrimiento, entrada única y bases de datos.

**Incluye.** Config server con un YAML por servicio; Eureka; gateway con rutas a `/api/usuarios`, `/api/roles`, `/api/lugares`, `/api/sectores`, `/api/eventos`, `/api/artistas`, `/api/entradas`, `/api/pagos` y `/api/devoluciones`; propagación de `X-User-Id` y `X-User-Role` a los servicios; docker-compose con PostgreSQL y una base por servicio; perfil para correr cada servicio solo (`spring.config.import=optional:configserver:`); README de ejecución.

**No incluye.** JWT (EP-12), circuit breaker (EP-13), log centralizado (EP-20) y Docker por servicio (EP-21).

**Criterios de aceptación de la épica**

- Con `docker compose up` existen las cuatro bases y los servicios arrancan contra ellas.
- Cada servicio de dominio arranca sin config server ni Eureka y sin depender del orden de inicio.
- Todo request externo entra por el gateway. Una ruta inexistente responde 404 y un servicio caído 503, con un cuerpo de error uniforme.
- Swagger de cada servicio abre en `/swagger-ui.html`.

**Riesgos.** Choque del puerto 8080 con Entrega3. Acoplar el arranque al orden config, eureka, servicios, gateway.

**Pruebas.** Smoke test por ruta en la colección de Postman.

### EP-02 · Usuarios y roles

**Entrega 1 · msvc-usuarios · P4 · Tamaño S · Change: `usuarios-roles`**

**Objetivo.** Gestionar usuarios y los cuatro roles del enunciado, que los demás servicios usan para validar identidad.

**Incluye.** Alta y consulta de usuarios (dni y email únicos). Roles CLIENTE, ORGANIZADOR, ADMINISTRADOR y PERSONAL\_ACCESO sembrados al iniciar de forma idempotente. Asignación de roles. La consulta que usa `msvc-evento` por Feign para validar que un organizador existe y tiene ese rol.

**No incluye.** Login y JWT (EP-12).

**Criterios de aceptación de la épica**

- Un dni o email repetido devuelve 409.
- Reiniciar el servicio no duplica los roles.
- Consultar un usuario devuelve sus roles, sin exponer entidades JPA.
- Un id inexistente devuelve 404.

**Riesgos.** Es dependencia de EP-04 y EP-06: conviene publicar su contrato primero.

**Pruebas.** Unitarias de unicidad y de asignación de roles.

### EP-03 · Lugares y sectores

**Entrega 1 · msvc-ubicacion · P4 · Tamaño M · Change: `lugares-sectores` · Historia: US-01**

**Objetivo.** Modelar los lugares y su estructura de sectores, que es la base de la disponibilidad y de los precios.

**Incluye.** Lugar (nombre, dirección, ciudad, latitud y longitud). Sector con tipo NUMERADO o NO\_NUMERADO: en numerados, filas y asientos por fila; en no numerados, capacidad máxima. Validaciones y consulta de un lugar con sus sectores. Uso opcional del servicio de mapas externo.

**No incluye.** Precios (viven en evento) ni disponibilidad (vive en entrada).

**Criterios de aceptación de la épica**

- Un lugar sin sectores o con coordenadas inválidas se rechaza con 400 y el detalle de cada error.
- En un sector numerado la capacidad es filas por asientos por fila.
- Un sector no numerado exige capacidad mayor a cero.
- No se puede borrar un sector referenciado por un evento.

**Riesgos.** Otros servicios guardan `id_sector` sin FK, así que los ids deben ser estables.

**Pruebas.** Unitarias de validación y de cálculo de capacidad.

### EP-04 · Eventos, precios y publicación

**Entrega 1 · msvc-evento · P3 · Tamaño L · Change: `catalogo-eventos` · Historias: US-02 a US-06**

**Objetivo.** Que un organizador pueda crear, configurar y publicar eventos, y que un administrador pueda moderarlos.

**Incluye.** Evento en borrador con artistas (con tipo: artista, equipo o disertante). `precio_sector_evento`. Validación por Feign de que el organizador existe y es ORGANIZADOR, y de que el lugar existe (de ahí se copia la ciudad). Publicación con precondiciones. Suspensión y cancelación por el administrador. Máquina de estados BORRADOR, PUBLICADO, AGOTADO, CANCELADO y FINALIZADO, con proceso programado para FINALIZADO. `alta_demanda` y `limite_por_cliente`.

**No incluye.** Disponibilidad de asientos (EP-06) ni la cache (EP-19).

**Criterios de aceptación de la épica**

- Publicar un evento sin todos los precios devuelve 409 con la lista de faltantes.
- Solo el organizador dueño edita su evento; otro recibe 403.
- Una transición de estado inválida se rechaza y queda registrado quién suspendió o canceló y por qué.
- Al publicar, `msvc-entrada` recibe lo necesario para inicializar asientos y zonas.
- No se puede cambiar el precio de un sector con entradas vendidas.

**Dependencias y riesgos.** Requiere EP-02 y EP-03. El contrato de inicialización de disponibilidad con `msvc-entrada` debe cerrarse con P1 antes de empezar. Las llamadas Feign son síncronas: definir timeouts desde ahora.

**Pruebas.** Unitarias de la máquina de estados y de las precondiciones de publicación.

### EP-05 · Búsqueda y detalle de eventos

**Entrega 1 · msvc-evento · P3 · Tamaño M · Change: `busqueda-eventos` · Historias: US-07 a US-09**

**Objetivo.** Que el cliente encuentre eventos rápido por múltiples criterios.

**Incluye.** Filtros combinables por tipo, organizador, participante, ciudad y rango de fechas. Orden por fecha, precio (mínimo entre sectores) o relevancia (coincidencia de texto en nombre y participantes, luego cercanía de fecha). Paginación con total. Solo eventos PUBLICADO o AGOTADO. Detalle con sectores y precios. Consultas aisladas en un repositorio de lectura para poder migrarlas o cachearlas en la entrega 2.

**No incluye.** El estado de cada asiento: vive en `msvc-entrada` y el cliente lo consulta aparte.

**Criterios de aceptación de la épica**

- Los filtros se combinan con AND y una búsqueda sin coincidencias devuelve 200 con lista vacía.
- El orden es estable y la paginación devuelve el total de resultados.
- Un evento en BORRADOR o CANCELADO no aparece.
- Las columnas filtradas tienen índices y la búsqueda responde en tiempo razonable sobre los datos de prueba de volumen.

**Riesgos.** El precio mínimo exige un join con `precio_sector_evento`. La definición de relevancia es propia y conviene validarla con la cátedra.

**Pruebas.** Unitarias de filtros, orden y paginación; prueba sobre el dataset grande.

### EP-06 · Disponibilidad y reserva temporal

**Entrega 1 · msvc-entrada · P1 · Tamaño XL · Change: `reserva-asientos` · Historias: US-10, US-11, US-14, US-15**

**Objetivo.** Que un cliente reserve lugares por 10 minutos mientras paga, y que esos lugares vuelvan a estar disponibles si no paga.

**Incluye.** Modelo de disponibilidad: entrada en estado RESERVADA con `expira_en` (no hay tabla `reserva`) y `zona_evento` para zonas no numeradas. Mecanismo de inicialización desde evento y ubicación (decidirlo y documentarlo en un ADR). Reserva atómica de asientos (todo o nada) y de zonas. Vencimiento con proceso programado y validación al leer. Límite por cliente y evento (cuentan reservas vigentes y entradas válidas, no las devueltas). Consulta de disponibilidad por evento y sector.

**No incluye.** El cobro (EP-08), la garantía anti doble venta (EP-07) y la fila virtual (EP-15).

**Criterios de aceptación de la épica**

- Reservar varios asientos es todo o nada: si uno está tomado, no se reserva ninguno y la respuesta es 409.
- Una reserva sin pago a los 10 minutos libera sus lugares y pasa a VENCIDA, y pagarla después se rechaza.
- En zonas, la reserva descuenta del contador y nunca supera la capacidad.
- Superar el límite por cliente devuelve 409.
- La disponibilidad nunca se sirve desde cache.

**Dependencias y riesgos.** Requiere EP-04 (inicialización). Usar fechas en UTC (`Instant`). El proceso de vencimiento tiene que ser seguro con varias instancias: liberar con un UPDATE condicional por estado.

**Pruebas.** Unitarias de reserva, vencimiento y límite; integración con PostgreSQL real.

### EP-07 · No doble venta e idempotencia

**Entrega 1 · msvc-entrada · P1 · Tamaño L · Change: `compra-idempotente` (junto con P2) · Historias: US-17 a US-19**

**Objetivo.** Garantizar que una misma entrada nunca se venda dos veces, aun con compras simultáneas, varias instancias o reintentos.

**Incluye.** Índice único parcial sobre (evento, sector, fila, número) para entradas RESERVADA, VALIDA o USADA. UPDATE condicional en zonas, con CHECK de que reservadas más vendidas no superen la capacidad. Clave de idempotencia (`Idempotency-Key`) en reserva y pago. ADR que explique el mecanismo elegido. Test de concurrencia con Testcontainers.

**No incluye.** Compensaciones entre servicios (EP-13).

**Criterios de aceptación de la épica**

- Con N pedidos simultáneos sobre el mismo asiento, exactamente uno tiene éxito y los demás reciben 409.
- Con dos instancias del servicio contra la misma base, el resultado es el mismo.
- Con 1 lugar libre en una zona y N pedidos simultáneos, solo uno tiene éxito.
- Reintentar con la misma clave devuelve el mismo resultado sin duplicar orden, cobro ni entradas; la misma clave con otro cuerpo devuelve 422.
- Un asiento vencido o devuelto puede volver a venderse.

**Riesgos.** H2 no reproduce el comportamiento de los locks de PostgreSQL, así que el test usa Testcontainers. Reservar varios asientos en orden distinto puede generar deadlocks: bloquearlos siempre en el mismo orden.

**Pruebas.** El test de concurrencia es el entregable estrella de la entrega 1, y lo revisa P5.

### EP-08 · Pago y emisión de entradas

**Entrega 1 · msvc-entrada · P2 · Tamaño L · Historias: US-12, US-13, US-16**

**Objetivo.** Cobrar la reserva y entregar al cliente sus entradas con QR.

**Incluye.** Pasarela mock configurable (aprueba, rechaza, demora o falla). `pago` y `registro_pago` con el detalle y el medio de pago. Pago aprobado: entradas VALIDA con QR único. Pago rechazado: se liberan los lugares. Regla para el pago aprobado que llega tras el vencimiento (reintegro automático). Consulta de mis entradas y de mis compras. Evento AGOTADO cuando se vende el último lugar.

**No incluye.** Reintentos y circuit breaker hacia la pasarela (EP-13).

**Criterios de aceptación de la épica**

- Un pago aprobado emite una entrada por lugar, cada una con QR único, y la compra queda registrada con su detalle.
- Un pago rechazado no emite entradas y libera los lugares de inmediato.
- Nunca se llama a la pasarela dentro de una transacción abierta.
- Un cliente solo ve sus propias entradas.
- Un pago cobrado sin entradas emitidas queda en un estado recuperable.

**Riesgos.** Cobrar y no poder registrarlo: dejar el pago en PENDIENTE y reconciliar.

**Pruebas.** Unitarias de los tres caminos (aprobado, rechazado, vencido) y de la unicidad del QR.

### EP-09 · Devoluciones y cancelación de eventos

**Entrega 1 · msvc-entrada y msvc-evento · P2 y P3 · Tamaño L · Change: `devoluciones` · Historias: US-20 a US-22**

**Objetivo.** Permitir devolver entradas con control del administrador y reintegrar a todos los compradores si un evento se cancela.

**Incluye.** Solicitud de devolución hasta 72 horas antes. Resolución por el administrador. Al aprobar, la entrada deja de estar activa, el lugar vuelve a estar disponible y se pide el reintegro al mock con su `referencia_pasarela`. Al cancelar un evento, `msvc-evento` avisa y `msvc-entrada` pasa todas sus entradas a EN\_REINTEGRO y pide los reintegros, tolerando fallos parciales.

**No incluye.** El envío de notificaciones (EP-14).

**Criterios de aceptación de la épica**

- Una devolución a menos de 72 horas, o de una entrada usada o ya devuelta, se rechaza con 409.
- Solo el administrador resuelve, y una solicitud resuelta no se vuelve a resolver.
- Al aprobar, el asiento o la zona vuelven a estar disponibles.
- Al cancelar el evento, todas las entradas quedan invalidadas y un fallo en un reintegro no frena los demás ni se pierde.

**Riesgos.** Un reintegro masivo puede ser de miles de entradas: procesarlo por lotes y de forma idempotente.

**Pruebas.** Unitarias de la regla de 72 horas y de los estados; prueba de cancelación con muchas entradas.

### EP-10 · Contratos, documentación y datos de prueba

**Entrega 1 · todos los servicios · P5 con todos · Tamaño M**

**Objetivo.** Que lo construido se pueda entender, ejecutar y probar sin ayuda del equipo.

**Incluye.** OpenAPI por servicio con descripciones, ejemplos y códigos de error. Colección de Postman con los flujos completos (compra feliz, pago rechazado, reserva vencida, devolución). Datos de prueba, con un estadio grande generado por script. Diagramas versionados en `docs/` (módulos, secuencia de la compra numerada con sus dos caminos de falla, ER por servicio con entidades solapadas). README con instrucciones de ejecución y ADRs.

**Criterios de aceptación de la épica**

- Cada endpoint está documentado con request, response y errores.
- La colección de Postman corre de punta a punta sobre los datos de prueba.
- Los diagramas están en texto (Mermaid) dentro del repositorio.
- Una persona ajena al equipo levanta el sistema siguiendo solo el README.

**Riesgos.** Documentación desactualizada: actualizarla en cada change de OpenSpec.

### EP-11 · Estrategia de testing de la entrega 1

**Entrega 1 · todos los servicios · Cada dueño, P5 revisa · Tamaño M**

**Objetivo.** Probar toda regla de negocio y demostrar la no doble venta.

**Incluye.** Unitarias de compra, devoluciones y búsqueda. Test de concurrencia (EP-07). Pruebas de capa web por controller. Integración con PostgreSQL mediante Testcontainers. Ejecución con `mvn verify` en cada push.

**Criterios de aceptación de la épica**

- Toda regla de negocio tiene al menos un test, y ningún test se modifica para que pase.
- La suite corre sin servicios externos levantados a mano.
- Cada servicio de dominio tiene tests en `domain`, `application` e `infrastructure/web`.

## Entrega 2: sistema completo

### EP-12 · Seguridad con JWT y roles

**Entrega 2 · gateway y todos los servicios · P5 y P4 · Tamaño L · Historia: US-34**

**Objetivo.** Reemplazar la identificación por header por autenticación y autorización reales.

**Incluye.** Validación del JWT en el gateway, propagación de la identidad, autorización por rol en cada endpoint según una matriz definida, esquemas de seguridad en OpenAPI y servicios no accesibles sin pasar por el gateway.

**Criterios de aceptación de la épica**

- Token ausente, vencido o inválido devuelve 401; un rol no autorizado devuelve 403.
- Existe una matriz rol por endpoint y un test automatizado que la verifica completa.
- El cambio no obliga a modificar la lógica de negocio de ningún servicio.
- No hay secretos commiteados.

**Riesgos.** Gestión de claves y expiración de tokens.

### EP-13 · Consistencia entre servicios y resiliencia

**Entrega 2 · msvc-entrada y clientes Feign · P1 y P2 · Tamaño XL · Historia: US-36**

**Objetivo.** Que la compra se mantenga consistente cuando un paso falla, y que el sistema sobreviva a la caída de servicios externos.

**Incluye.** Saga de compra con compensaciones documentadas (qué se deshace en cada falla). Reintentos con backoff, timeouts y circuit breaker con fallback hacia la pasarela, notificaciones y las llamadas Feign a evento y usuarios. Justificación de consistencia eventual contra fuerte. ADRs de los patrones usados.

**Criterios de aceptación de la épica**

- Con la pasarela caída o lenta, la compra termina en un estado controlado y la reserva se libera si corresponde.
- Una compra que falla a mitad de camino no deja lugares bloqueados ni cobros sin entrada.
- Hay pruebas de fallo para cada dependencia externa.

**Riesgos.** Compensaciones que también fallan: deben ser idempotentes y reintentables.

### EP-14 · Mensajería y notificaciones

**Entrega 2 · msvc-notificaciones (nuevo) y broker · P4 · Tamaño L · Historias: US-30 a US-32**

**Objetivo.** Avisar a los clientes de lo que les importa sin acoplar ni frenar la compra.

**Incluye.** Broker (RabbitMQ). Eventos de dominio publicados por cada servicio, con el catálogo de `docs/`. Consumidores idempotentes. Avisos por compra confirmada, evento cancelado o modificado, y artista favorito que publica en la ciudad del cliente. Reintentos y cola de errores. Envío por email o push (simulado).

**Criterios de aceptación de la épica**

- Una falla del servicio de notificaciones no afecta a la compra ni a la devolución.
- Un mismo evento entregado dos veces no genera dos avisos.
- Cada cliente recibe solo los avisos que corresponden a sus preferencias.

**Dependencias.** EP-17 para los artistas favoritos.

### EP-15 · Fila virtual

**Entrega 2 · msvc-fila (nuevo, Redis) · P1 · Tamaño L · Historias: US-23 a US-25**

**Objetivo.** Ordenar el ingreso a la compra de eventos masivos de forma justa.

**Incluye.** Fila por evento en Redis, posición por orden de llegada, tandas, token de turno con vencimiento y verificación del turno antes de reservar. Versión simplificada (consulta de posición por polling) con la simplificación justificada.

**Criterios de aceptación de la épica**

- Bajo carga, el orden de la fila respeta estrictamente el orden de llegada.
- Un cliente sin turno no puede reservar y un turno no usado vence y libera cupo.
- Reingresar a la fila conserva la posición.

### EP-16 · Control de acceso con QR

**Entrega 2 · msvc-entrada · P2 · Tamaño M · Historias: US-26 y US-27**

**Objetivo.** Que el personal de acceso valide entradas en la puerta sin dejar pasar dos veces el mismo QR.

**Incluye.** Validación de que la entrada existe, corresponde al evento y no fue usada. Marcado como USADA de forma atómica (UPDATE condicional o `@Version`). Rol PERSONAL\_ACCESO. Respuesta con el motivo del rechazo. Registro de cada ingreso en el log de operaciones críticas.

**Criterios de aceptación de la épica**

- El mismo QR escaneado a la vez desde dos lectores es aceptado una sola vez.
- Un QR inexistente, alterado, de otro evento, devuelto o de un evento cancelado se rechaza con su motivo.
- Un usuario sin el rol recibe 403.

### EP-17 · Preferencias y recomendaciones

**Entrega 2 · msvc-recomendaciones (nuevo, MongoDB) · P3 · Tamaño L · Historias: US-28 y US-29**

**Objetivo.** Personalizar la oferta según los gustos y el historial de cada cliente.

**Incluye.** Preferencias (tipos, artistas favoritos, ciudad) y historial alimentado por eventos de compra, en MongoDB. Recomendaciones por afinidad y por co-ocurrencia (quienes compraron este evento también compraron). Fallback con los eventos más populares de la ciudad. Justificación escrita de MongoDB: documentos con listas variables y esquema cambiante.

**Criterios de aceptación de la épica**

- Un cliente con historial recibe eventos vigentes afines, sin los que ya compró.
- Un cliente sin historial recibe los más populares de su ciudad.
- Si el motor falla, la compra no se ve afectada.

**Nota.** El enunciado permite una versión simplificada, justificada.

### EP-18 · Reportes para organizadores

**Entrega 2 · msvc-reportes (nuevo, o módulo de entrada si se simplifica) · P4 · Tamaño M · Historia: US-33**

**Objetivo.** Que cada organizador vea cómo van sus eventos.

**Incluye.** Entradas vendidas y devueltas por sector, porcentaje de ocupación y recaudación neta, calculados a partir de eventos de compra y devolución.

**Criterios de aceptación de la épica**

- La recaudación descuenta las devoluciones aprobadas.
- Un organizador solo consulta sus propios eventos; otro recibe 403.
- Los números coinciden con los de la base de `msvc-entrada` en el mismo momento.

### EP-19 · Cache

**Entrega 2 · msvc-evento y gateway · P3 · Tamaño M · Historia: US-37**

**Objetivo.** Acelerar las consultas de lectura sin servir datos vencidos donde importan.

**Incluye.** Cache del detalle de eventos, del catálogo y de los resultados de búsqueda, con TTL definido e invalidación al publicar, modificar o cancelar. Documento que explique qué se cachea, dónde y por qué no se cachea la disponibilidad.

**Criterios de aceptación de la épica**

- Cambiar un evento invalida su cache de inmediato.
- La disponibilidad de asientos nunca viene de cache.
- Se mide el porcentaje de aciertos y el tiempo de respuesta con y sin cache.

### EP-20 · Trazabilidad y log centralizado

**Entrega 2 · todos los servicios · P5 · Tamaño M · Historia: US-35**

**Objetivo.** Poder reconstruir cualquier operación crítica a través de los servicios.

**Incluye.** Log de cada request (endpoint, usuario, código y tiempo). Identificador de correlación generado en el gateway y propagado por Feign y por mensajes. Log centralizado con las operaciones críticas (compra, devolución, cancelación e ingreso).

**Criterios de aceptación de la épica**

- Buscando un identificador de correlación se ven todos los pasos de una compra en orden y en todos los servicios.
- Ningún log contiene datos sensibles ni tokens.

### EP-21 · Despliegue y prueba de carga

**Entrega 2 · todos los servicios · P5 · Tamaño L · Historia: US-38**

**Objetivo.** Demostrar que el sistema se despliega con un comando y soporta la apertura de venta de un evento masivo.

**Incluye.** Un Dockerfile por servicio (build multietapa con la raíz como contexto). Un `docker-compose.yml` con bases, broker, Redis, gateway y servicios, con healthchecks y orden de arranque. Pruebas de integración entre servicios. Prueba de carga con JMeter sobre el flujo de compra, con informe de usuarios concurrentes, tiempos (p95) y errores.

**Criterios de aceptación de la épica**

- `docker compose up` levanta todo el sistema desde cero.
- La prueba de carga se ejecuta sobre un evento con muchos asientos y su informe está en el repositorio.
- Después de la prueba, ningún asiento aparece vendido dos veces.

### EP-22 · Interfaz gráfica (opcional)

**Entrega 2 · frontend · A definir · Tamaño L · Prioridad: deseable**

**Objetivo.** Cubrir los flujos principales del cliente a través del gateway.

**Incluye.** Buscar eventos, elegir sector y asientos, comprar y ver mis entradas, con React, Angular, Vue o Thymeleaf. Instrucciones de ejecución en el README.

**Criterios de aceptación de la épica**

- Un cliente completa el flujo de compra sin usar Postman.
- La interfaz solo habla con el gateway.

**Nota.** Se aborda solo si el núcleo y los aspectos no funcionales están cerrados.
