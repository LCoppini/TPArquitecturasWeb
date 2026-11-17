# Modelo sugerido para msvc-recomendacion

Las tres entidades —Preferencia, Recomendacion y Notificacion— pertenecen al
mismo microservicio `msvc-recomendacion`. Se pueden modelar como documentos
persistentes en MongoDB y relacionar mediante identificadores, sin claves
foráneas entre microservicios.

## Preferencia

Se propone un documento por cliente, identificado por `idUsuario`.

| Atributo | Tipo | Responsabilidad |
|---|---|---|
| `idUsuario` | `Long` | Identifica al cliente y funciona como ID del documento. |
| `tiposEvento` | Lista de textos | Tipos de eventos que le interesan al cliente. |
| `artistasFavoritos` | Lista de IDs (`Long`) | Identificadores de artistas favoritos, pertenecientes al catálogo de `msvc-evento`. |
| `ciudad` | Texto | Ciudad usada para personalizar recomendaciones y notificaciones. |
| `historialCompras` | Lista de compras | Historial recibido mediante eventos; cada compra conserva el ID único del mensaje, el ID del evento comprado, la fecha y datos relevantes como tipo y artistas. |
| `actualizadaEn` | `Instant` | Fecha de la última modificación de las preferencias. |

**Responsabilidad:** conservar las preferencias y las señales del historial que
necesita el motor de recomendaciones. El historial se alimentaría de forma
asíncrona con eventos de compra y debería deduplicarse usando el identificador
del evento recibido.

## Recomendacion

Se propone un documento por lote de recomendaciones generado para un cliente.
Los resultados se guardarían dentro del documento como una lista de elementos.

| Atributo | Tipo | Responsabilidad |
|---|---|---|
| `id` | `String` u `ObjectId` | Identificador del lote de recomendaciones. |
| `idUsuario` | `Long` | Cliente para quien se generó el lote. |
| `generadaEn` | `Instant` | Fecha y hora en que se calcularon las recomendaciones. |
| `ciudad` | Texto | Ciudad usada para seleccionar eventos. |
| `criterio` | Texto | Método principal aplicado: afinidad, co-ocurrencia o fallback popular. |
| `eventos` | Lista de eventos recomendados | Resultados con ID del evento, nombre y tipo como snapshot, fecha, puntuación y motivo de recomendación. |
| `expiraEn` | `Instant` opcional | Permite considerar desactualizado el lote después de una fecha determinada. |

**Responsabilidad:** registrar qué sugerencias se entregaron y por qué. Cada
evento recomendado referencia a `msvc-evento` mediante su ID; puede conservar
los datos mínimos de presentación como snapshot para representar el resultado
generado aunque el catálogo cambie.

## Notificacion

Se propone un documento por aviso destinado a un cliente.

| Atributo | Tipo | Responsabilidad |
|---|---|---|
| `id` | `String` u `ObjectId` | Identificador de la notificación. |
| `idUsuario` | `Long` | Identifica al destinatario. |
| `tipo` | Texto | Motivo del aviso: compra confirmada, evento cancelado o modificado, o artista favorito que publica en la ciudad del cliente. |
| `idEvento` | `Long` opcional | Referencia al evento relacionado, si corresponde. |
| `idEventoOrigen` | Texto | Identificador del evento de dominio que generó el aviso; permite evitar notificaciones duplicadas. |
| `contenido` | Objeto o mapa | Datos mínimos necesarios para presentar el aviso. |
| `canal` | Texto | Canal de entrega, por ejemplo email o push simulado. |
| `estado` | Texto | Estado del proceso: pendiente, enviada o fallida. |
| `creadaEn` | `Instant` | Fecha de creación de la notificación. |
| `enviadaEn` | `Instant` opcional | Fecha de envío, si se completó. |
| `intentos` | Número entero | Cantidad de intentos de entrega realizados. |

**Responsabilidad:** procesar y registrar avisos de forma asíncrona e
idempotente, sin bloquear el flujo de compra ni el de devoluciones.

## Relaciones

- **Preferencia 1 a N Recomendacion:** ambos documentos comparten `idUsuario`;
  cada recomendación representa una generación para ese cliente.
- **Preferencia 1 a N Notificacion:** `idUsuario` identifica al cliente y sus
  preferencias pueden determinar qué avisos le corresponden.
- **Recomendacion hacia eventos:** cada elemento contiene un `idEvento` que
  referencia al catálogo de `msvc-evento`; no existe acceso directo a la base
  de datos de ese servicio.
- **Notificacion hacia su origen:** puede incluir `idEvento` y conserva
  `idEventoOrigen` para relacionar el aviso con el evento de dominio que lo
  produjo y evitar duplicados.
- **Recomendacion y Notificacion no requieren una relación directa:** ambas se
  vinculan al cliente, pero una notificación también puede originarse en una
  compra o en la modificación de un evento.

De esta manera, las tres entidades pertenecen a un único microservicio y
mantienen responsabilidades diferenciadas, relacionándose mediante
identificadores.
