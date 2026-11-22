# Design

## Context

Ver `proposal.md` para la motivación. Estado que condiciona este diseño:

- `msvc-preferencias` (8086, MongoDB) es un esqueleto: solo `package-info.java`, `pom.xml` y `application.yml`. No hay lógica, entidades ni endpoints.
- El gateway ya tiene las rutas `preferencias-route` y `recomendaciones-route` hacia `lb://msvc-preferencias`, y `GatewayRoleMatrix` ya declara `CLIENTE_Y_ADMIN` para ambos prefijos; este change lo sustituye por rutas separadas por rol. `SecurityConfig` ya deniega `/internal/**`.
- `msvc-entrada` aprueba el pago dentro de `PagoService.crear()` (`@Transactional`): pasa la entrada a `VALIDA`, genera el QR y guarda el `RegistroPago`. Ya invoca a `UsuarioClient` y `EventoClient` durante la reserva, pero no tiene `@EnableAsync`, `@EnableScheduling` ni Resilience4j.
- Los DTOs Feign locales de `msvc-entrada` están recortados: `EventoResponse` solo trae `id`, `fechaHora` y `estado`; `UsuarioResponse` solo trae `idUsuario`. Los servicios dueños sí devuelven `tipo`, `participantes` y `ciudad`.
- No hay timeouts de Feign configurados en ningún YAML: solo `httpclient.enabled: true`, con defaults muy por encima del TimeLimiter previsto.
- ADR-0001 define que el gateway inyecta `X-User-Id` desde el claim `idUsuario`, descarta `X-User-Role`, y que los servicios conservan solo la comparación de pertenencia por `X-User-Id`.

## Goals / Non-Goals

**Goals:**

- Registrar gustos automáticamente a partir de compras aprobadas, sin que una falla de preferencias afecte la compra.
- Tolerar caídas, reintentos, reinicios y dos instancias de `msvc-entrada` sin duplicar ni perder avisos.
- Servir recomendaciones explicables con un fallback determinístico.
- Mantener el modelo de seguridad vigente: gateway para roles, servicio para pertenencia.

**Non-Goals (design-level):**

- No se crea un microservicio nuevo ni una base nueva distinta de la ya prevista para preferencias.
- No se implementa co-ocurrencia, popularidad ni cálculo de ventas.
- No se modifica `msvc-notificaciones` ni su modelo de reintentos, aunque el patrón sea el mismo.
- No se cambia `sinFiltros` ni `EventoServiceImpl.buscar()`; el arreglo de eficiencia se tramita en otro change.
- No se introduce RabbitMQ, Caffeine, Redis ni un evento `UsuarioActualizado`.

## Decisions

### 1. La recomendación vive dentro de `msvc-preferencias`

No existe `msvc-recomendaciones`. El gateway ya apunta los prefijos de cliente al mismo service ID y este change agrega los prefijos administrativos hacia ese mismo ID; dos `@RestController` distintos separan semánticamente consulta de preferencias y de recomendaciones, cada uno con su variante de cliente y de administrador.

- **Alternativa descartada**: microservicio separado. Costo de despliegue, base y Eureka sin beneficio: comparte exactamente el mismo documento de preferencias.

### 2. Registro REST asíncrono con outbox persistido

Al aprobar el pago se inserta un row en `pendientes_preferencia` **dentro de la transacción de `PagoService.crear()`**. Un worker lo entrega por Feign a preferencias. La llamada nunca está en el camino del cliente.

- **Alternativa descartada — llamada síncrona**: si preferencias cae, falla la compra. Inaceptable.
- **Alternativa descartada — `@Async` sin persistencia**: el task muere con el proceso; se pierde la preferencia.
- **Alternativa descartada — RabbitMQ**: nueva tecnología, requiere ADR y broker; el volumen no lo justifica.

### 3. El worker enriquece el DTO

La fila pendiente guarda solo `id_entrada`, `id_usuario` e `id_evento`. El worker consulta `EventoClient` y `UsuarioClient` para completar tipo, artistas y ciudad, y hace el POST al contrato interno.

- **Alternativa descartada — `PagoService` arma el DTO**: agregaría dos llamadas Feign nuevas al camino crítico del pago.
- **Alternativa descartada — snapshot en la reserva**: ya están los datos en `EntradaService:67-68`, pero obliga a agregar columnas o una segunda tabla con estado "esperando pago".
- **Trade-off aceptado**: si se editan los participantes del evento entre la compra y el envío, se registra la versión vigente al momento del envío. Se acepta porque preferencias nunca consulta eventos por su cuenta.

### 4. Contrato interno y extendido

```java
public record CompraRegistradaRequest(
        @NotNull Long idUsuario,
        @NotNull Long idEntrada,      // idempotencia
        @NotNull Long idEvento,
        @NotBlank String tipoEvento,
        @NotBlank String ciudad,
        List<ArtistaCompraRequest> artistas
) {}

public record ArtistaCompraRequest(@NotNull Long idArtista, @NotBlank String nombre) {}
```

- Todos los participantes del evento se tratan como artistas: `Participante.tipo` es texto libre (`Banda`, `Grupo`, `Artista invitado`), no un discriminador. No se filtra.
- No viajan medio de pago, monto, QR ni ubicación del asiento: ya están en `Pago`/`RegistroPago` y no expresan gusto.

Los DTOs locales `EventoResponse` y `UsuarioResponse` de `msvc-entrada` se extienden con los campos faltantes y se agrega un `ParticipanteResponse` local. Se conservan **constructores sobrecargados con las firmas actuales** (`UsuarioResponse(Long)` y `EventoResponse(Long, Instant, String)`) para que los 5 usos existentes en tests sigan compilando sin modificar ningún test. Jackson usa el constructor canónico del record.

- **Alternativa descartada**: DTOs nuevos dedicados duplicarían campos y clients.

### 5. Idempotencia en el documento MongoDB

`PreferenciaCliente` usa `_id = idUsuario`, `ciudad`, `Map<String,Integer> tiposEvento`, `List<ArtistaPreferido>` con `cantidad`, `Set<Long> comprasProcesadas`, `Set<Long> eventosComprados` y `actualizadoEn`.

El registro es un único `updateOne` atómico con filtro condicional:

```
filtro: { _id: 42, comprasProcesadas: { $ne: 101 } }
```

El filtro hace idempotente el reintento en el que el POST anterior tuvo éxito pero se perdió la confirmación: si la entrada ya está en `comprasProcesadas`, la actualización no produce cambios.

Para que un artista nuevo se agregue **y** uno existente se incremente sin dos escrituras separadas, se usa un pipeline de actualización de MongoDB (disponible desde 4.2; el proyecto usa MongoDB 7) con estos pasos:

1. `$set` de `comprasProcesadas` con `$setUnion` entre el conjunto actual y `[idEntrada]`; `$set` de `eventosComprados` con `$setUnion` entre el actual y `[idEvento]`; `$set` de `ciudad` y `actualizadoEn`.
2. `$set` de `artistas`: si `idArtista` ya figura en `artistas.idArtista`, un `$map` incrementa en uno el `cantidad` de ese elemento; si no figura, `$concatArrays` agrega `{ idArtista, nombre, cantidad: 1 }`.
3. `$set` de `tiposEvento`: mismo patrón sobre `$objectToArray` / `$map` / `$arrayToObject`, para incrementar el tipo existente o crearlo con valor `1`.

Al ser un solo `updateOne`, los tres pasos son atómicos para el documento. MongoDB serializa las escrituras sobre el mismo `_id`, por lo que dos compras concurrentes del mismo cliente no pierden incrementos; el filtro por `idEntrada` evita que la misma entrada se aplique dos veces.

- **Alternativa descartada — dos pasos `$addToSet` + `$inc` con `arrayFilters`**: `$addToSet` compara el subdocumento completo, por lo que un artista cuyo `cantidad` ya cambió no coincide con el valor inicial y volvería a insertarse. Exigiría además un índice multikey único sobre `artistas.idArtista` y no resolvería la carrera entre dos compras concurrentes del mismo artista nuevo.
- **Alternativa descartada — `$inc` solo con `arrayFilters`**: incrementa artistas existentes pero nunca crea uno nuevo, que era el defecto del borrador anterior.

Los tests requeridos son: primera compra con artista nuevo, segunda compra con el mismo artista ya existente, dos compras concurrentes con el mismo artista nuevo, reintento con la misma `idEntrada` y verificación de que `eventosComprados` queda poblado con `idEvento`.

### 6. Regla de recomendación

- Mínimo de un compra por artista: cualquier compra cuenta como favorito.
- Al recomendar: top 5 artistas y top 3 tipos, ordenados por `cantidad DESC`, desempate por identificador ascendente.
- Motivos: `MISMO_TIPO`, `MISMO_ARTISTA`, `FALLBACK_CIUDAD`.
- Exclusión de eventos ya comprados (EP-17): todo candidato cuyo `idEvento` esté en `eventosComprados` se descarta, tanto en las coincidencias por tipo/artista como en el fallback por ciudad.
- Fallback: si no hay documento, se obtiene la ciudad del usuario; siempre con `desde=<ahora>` y `orden=fecha`, porque la búsqueda con filtros de `msvc-evento` puede incluir eventos pasados.
- La devolución no descuenta: es una decisión de alcance, no un comportamiento activo. No existe notificación de devoluciones hacia preferencias.

### 7. Outbox: tres fases y multi-instancia

1. **Claim** (`@Scheduled`): un `UPDATE` atómico toma el registro más viejo elegible y lo pasa a `EN_PROCESO`.
2. **Llamada** (`@Async`): fuera de toda transacción, con `ThreadPoolTaskExecutor` propio y `TaskDecorator` que copia el MDC.
3. **Resultado** (nueva transacción): un único `UPDATE ... WHERE id = :id AND estado = 'EN_PROCESO'` escribe `intentos`, `proximo_intento_en`, `ultimo_error` y `estado` juntos. Si otra instancia ya reclamó la fila, actualiza 0 filas y se descarta.

La recuperación de registros `EN_PROCESO` con más de 5 minutos evita avisos trabados si una instancia muere.

**Alternativa descartada**: envolver la llamada Feign en la transacción. Mantendría un bloqueo de base mientras se espera a otro servicio.

### 8. Reintentos y configuración de resiliencia

5 intentos reales en total (no 5 reintentos adicionales a un intento inicial), con backoff `10s / 30s / 1m / 5m` entre cada uno, todos configurables desde YAML. Si el circuito está abierto (`CallNotPermittedException`), no se cuenta como intento real: solo se corre `proximo_intento_en` con `waitDurationInOpenState + 5s`, derivado de la misma property para que no se desalineen. Al agotar el quinto intento real, el registro pasa a `FALLIDO` y se conserva para replay manual.

```yaml
ticketya:
  preferencias:
    outbox:
      intentos-maximos: 5
      backoff: 10s,30s,1m,5m
      stale-timeout: 5m
      poll-ms: 5s

resilience4j:
  circuitbreaker:
    instances:
      preferencias:
        slidingWindowType: TIME_BASED
        slidingWindowSize: 120       # segundos
        minimumNumberOfCalls: 3
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        automaticTransitionFromOpenToHalfOpenEnabled: true
  timelimiter:
    instances:
      preferencias:
        timeoutDuration: 2s

spring:
  cloud:
    openfeign:
      client:
        config:
          msvc-preferencias:
            connect-timeout: 500     # ms
            read-timeout: 1200       # ms -> suma 1700ms, 300ms bajo el TimeLimiter
```

- `TIME_BASED` con 120s y mínimo 3 llamadas: el tráfico es esporádico (solo compras aprobadas), y con `COUNT_BASED` el breaker podría no acumular muestras nunca.
- Sin `retry` interno de Resilience4j: el presupuesto de reintentos lo lleva únicamente el worker persistido. Si no, los intentos reales dejarían de ser reales.
- **Sin ADR adicional para resiliencia**: Resilience4j ya es dependencia real en `msvc-notificaciones/pom.xml` y ya está declarado como estrategia en `openspec/config.yaml`; el patrón `@Async` + `TaskDecorator` + MDC también está ahí descrito, y la comparación por `X-User-Id` viene de ADR-0001. Agregar el mismo starter al pom de `msvc-entrada` es aplicar decisiones existentes. El único ADR nuevo de este change es el `0004`, de MongoDB para `msvc-preferencias`.

### 9. Seguridad y endpoints finales

| Endpoint | Exposición | Regla |
|---|---|---|
| `POST /internal/preferencias/compras` | Sin ruta en gateway; Feign directo por Eureka | `idUsuario` del body, idempotente por `idEntrada` |
| `GET /api/preferencias/clientes/{idUsuario}` | Gateway, rol `CLIENTE` | El servicio compara `{idUsuario}` con `X-User-Id`; si difieren, 403 |
| `GET /api/admin/preferencias/clientes/{idUsuario}` | Gateway, rol `ADMINISTRADOR` | El servicio no compara; sirve para cualquier cliente |
| `GET /api/recomendaciones/clientes/{idUsuario}` | Gateway, rol `CLIENTE` | Igual comparación que preferencias |
| `GET /api/admin/recomendaciones/clientes/{idUsuario}` | Gateway, rol `ADMINISTRADOR` | Sin comparación; sirve para cualquier cliente |

El POST va bajo `/internal/**` siguiendo el patrón de `/internal/entradas/disponibilidad`. Si estuviera bajo `/api/preferencias/**`, la matriz de cliente permitiría a cualquier cliente falsificar compras.

La validación de pertenencia se implementa por separado en los dos controllers de cliente, comparando `@PathVariable` con `@RequestHeader("X-User-Id")`, igual que `EventoServiceImpl` compara `idOrganizador`. Los dos controllers administrativos no comparan: el rol ya fue resuelto por el gateway según el path. No se propaga `X-User-Role`, no se agrega otro header de rol y el servicio no consulta `msvc-usuarios` para reconstruirlo; así se respeta ADR-0001.

El gateway necesita cambios acotados: agregar `admin-preferencias-route` y `admin-recomendaciones-route` hacia `lb://msvc-preferencias`, incorporar esos prefijos al array `ADMIN` de `GatewayRoleMatrix`, mover `/api/preferencias/**` y `/api/recomendaciones/**` al array `CLIENTE` y retirar `CLIENTE_Y_ADMIN`. `prefijosConRegla()` y `GatewayRoleMatrixTest` se actualizan en consecuencia. El `denyAll()` de `/internal/**` ya existe y no se toca.

- **Alternativa descartada — un único path con detección de rol en el servicio**: exigiría propagar `X-User-Role` o consultar `msvc-usuarios`, contradiciendo ADR-0001.

### 10. Trazabilidad

El `correlation_id` del request del pago se guarda en la fila, el `TaskDecorator` lo restaura en el MDC del hilo async y un interceptor Feign lo envía como `X-Correlation-Id`. Cumple `openspec/config.yaml` y permite seguir la operación de punta a punta entre logs.

## Risks / Trade-offs

- **[Una caída prolongada puede mantener el aviso reintentando sin límite de tiempo absoluto]** → Mitigación: **limitación conocida y aceptada para esta entrega**; se documenta aquí y no se implementa ni se registra como trabajo pendiente. El presupuesto solo se agota con intentos reales; los rechazos por circuito abierto no lo consumen.
- **[Los datos del evento pueden cambiar entre la compra y el envío]** → Mitigación: se acepta la versión vigente al momento del envío; preferencias no consulta eventos ni ubicación.
- **[Desfase de ciudad con `msvc-usuarios`]** → La ciudad de la preferencia se actualiza solo al procesar una compra aprobada. Si el cliente cambia de ciudad en `msvc-usuarios` sin volver a comprar, el fallback por ciudad puede usar una ciudad desactualizada hasta la próxima compra. **Limitación conocida y aceptada para esta entrega**: no se consulta `usuarioClient` en cada recomendación para evitarla.
- **[Un registro queda EN_PROCESO si muere la instancia]** → Mitigación: recuperación automática después de 5 minutos y `WHERE estado = 'EN_PROCESO'` en la escritura del resultado.
- **[Pocas llamadas pueden evitar que el circuito abra]** → Mitigación: ventana temporal amplia y mínimo bajo; con tráfico esporádico no hay riesgo de saturar el destino.
- **[Carrera entre el timeout HTTP y el TimeLimiter]** → Mitigación: timeouts del cliente suman 1700ms contra 2000ms del TimeLimiter, con 300ms de margen.
- **[Modificar DTOs compartidos rompe tests existentes]** → Mitigación: constructores sobrecargados; se agrega un test de contrato nuevo sin editar los actuales.
- **[No hay integración con notificaciones]** → Mitigación: decisión de alcance; el endpoint de artistas para notificaciones queda documentado como futuro.

## Migration Plan

1. No hay migración de datos: la colección de preferencias es nueva y no hay documentos previos.
2. La tabla `pendientes_preferencia` se crea con `ddl-auto: update` en desarrollo, igual que el resto de las tablas del proyecto.
3. Despliegue ordenado: primero `msvc-preferencias` con el contrato interno y los endpoints públicos; después `msvc-entrada` con la tabla, el worker y la configuración de resiliencia; por último los casos de test del gateway.
4. Rollback: desactivar el worker por configuración y dejar de insertar avisos. Los avisos ya persistidos no se pierden y pueden reprocesarse manualmente. El contrato interno de preferencias queda inofensivo sin productor.
