# ADR-0004: MongoDB para msvc-preferencias

- Estado: aceptada
- Fecha: 2026-11-22

## Contexto

`msvc-preferencias` (Entrega 2, puerto 8086) debe guardar, por cliente, un perfil
de gustos derivado de sus compras aprobadas: tipos de evento, artistas favoritos
con su cantidad, ciudad, identificadores de entrada ya procesados e
identificadores de eventos ya comprados. El `openspec/config.yaml` ya define que
este servicio usa MongoDB y que no migra datos existentes; el README del módulo
tenía marcado como pendiente el ADR que justificara esa elección.

Las operaciones que debe resolver el almacenamiento son:

- Lectura y escritura del perfil **por `idUsuario`**, que es la clave natural del
  documento.
- Actualizaciones atómicas e idempotentes por `idEntrada` que incrementan
  contadores anidados, agregan artistas nuevos y registran eventos comprados.
- Consulta del perfil para armar recomendaciones; no hay consultas entre
  perfiles de distintos clientes ni agregaciones globales en esta entrega.
- No hay foreign keys entre servicios: los identificadores de evento, artista y
  entrada se referencian por valor, no por relaciones.

`AGENTS.md` exige un ADR para toda dependencia o tecnología nueva. MongoDB ya
está en el `pom.xml` del módulo desde el esqueleto, pero la decisión nunca había
quedado registrada.

## Decisión

Usar **MongoDB 7** como base de `msvc-preferencias`, con una colección de un
documento por cliente:

```
_id: idUsuario
ciudad: String
tiposEvento: Map<String, Integer>
artistas: [ { idArtista, nombre, cantidad } ]
comprasProcesadas: Set<Long>
eventosComprados: Set<Long>
actualizadoEn: Instant
```

La escritura se hace con un único `updateOne` atómico filtrado por
`comprasProcesadas.$ne: idEntrada`, usando un pipeline de agregación para agregar
o incrementar artistas y tipos en la misma operación. El detalle está en
`openspec/changes/recomendaciones-preferencias/design.md`, sección 5.

La recepción de compras se resuelve con REST interno + outbox persistido en
`msvc-entrada`, no con un broker; esa decisión se documenta en el mismo design y
en `msvc-preferencias/src/main/java/com/ticketya/preferencias/messaging/package-info.java`.

## Alternativas consideradas

1. **PostgreSQL con tablas relacionales** (`preferencia_cliente`,
   `artista_preferido`, `tipo_preferido`, `entrada_procesada`). Descartada: el
   acceso siempre es por cliente y no hay consultas con joins entre clientes;
   obligaría a varias tablas y a transacciones para mantener el mismo agregado
   que MongoDB representa en un documento.
2. **PostgreSQL con una columna JSONB**. Descartada: conservaría la base
   relacional del resto del proyecto, pero las actualizaciones anidadas
   idempotentes son más verbosas y el modelo seguiría siendo un documento
   guardado como texto, sin ganar una consulta relacional que se necesite.
3. **Sin persistencia, solo caché en memoria**. Descartada: se perdería el
   historial ante un reinicio y no se podría garantizar idempotencia con varias
   instancias.
4. **Agregar un broker (RabbitMQ/Kafka)** para recibir las compras. Descartado
   en este change: introduce nueva tecnología y un ADR adicional, y el volumen
   no justifica el broker frente al outbox ya persistido en `msvc-entrada`.

## Consecuencias

### Mejoras

- El perfil completo del cliente se lee y se escribe en una sola operación por
  `idUsuario`, sin joins.
- El esquema es flexible: agregar `eventosComprados` no requirió migración de
  columnas ni tablas nuevas.
- El update atómico con filtro por `idEntrada` da idempotencia incluso con reintentos
  del worker y varias instancias de `msvc-entrada`.

### Costos y riesgos

- Las consultas analíticas globales (por ejemplo, popularidad de artistas)
  exigirían índices o agregaciones; quedan fuera de este change.
- La consistencia con `msvc-entrada` es eventual: depende del outbox y de sus
  reintentos, no de una transacción distribuida.
- La ciudad se actualiza solo al procesar una compra; ese desfase se acepta y se
  documenta en el design.
- Se mantiene una segunda tecnología de base de datos en el sistema (PostgreSQL
  para los otros servicios, MongoDB aquí), con su correspondiente contenedor en
  `docker-compose.yml`.

## Referencias

- `AGENTS.md`, regla "Nuevas dependencias o tecnologías solo con un ADR"
- `openspec/config.yaml`, sección "Preferencias y recomendaciones"
- `openspec/changes/recomendaciones-preferencias/design.md`
- `openspec/changes/recomendaciones-preferencias/proposal.md`
- `msvc-preferencias/README.md`
