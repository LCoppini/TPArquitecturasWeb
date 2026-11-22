# Tareas

## Revisión previa, aprobación y dependencias

- [x] Revisar proposal, design y specs EARS con el grupo y aprobarlos antes de
      escribir código.
- [x] Confirmar las tres decisiones originales: `EN_REINTEGRO` no ocupa,
      sector sin capacidad se incluye con 0, y
      `totales.porcentajeOcupacion` se calcula sobre capacidad total.
- [x] Confirmar que no se modificarán tests existentes para que pasen, que no
      se tocará `NoDobleVentaTest`, que no se hará push y que no se agregará
      ADR.
- [x] Aplicar los ajustes aprobados: `entradasEmitidas` vs `entradasActivas`,
      `RegistroPago.montoTotal` por entrada, mock `REEMBOLSADO`, identidad en
      `GatewayErrorHandlingTest`, ruta genérica sin ruta nueva, 403/404,
      JSON coherente e integración con docker-compose.
- [x] Aprobar los ajustes del 2026-11-22: capacidad física de sectores
      numerados, `SectorClient` 404 vs 5xx, `/internal/**` 401/403, errores de
      paths y headers, agregación con `GROUP BY`, mock en `FALLA`, prueba con
      datos armados a mano, riesgo de `cantidadVendida()`, archivado sin
      esperar a EP-21 y orden de ejecución.
- [x] **Condición (a):** no iniciar la implementación hasta que el smoke test
      de login real esté verde: emisión RS256 en `msvc-usuarios`, validación
      en el gateway y llegada de `X-User-Id` al servicio destino.
- [x] **Condición (b):** no iniciar la implementación hasta que esté integrado
      el commit de `msvc-evento` de separación de paths por rol, que toca
      `organizador-eventos-route` y la eliminación de `validarRol`.

## Corrección previa en msvc-ubicacion (commit propio)

- [x] Verificar el estado actual: `UbicacionMapper` ya calcula
      `capacidadMax = filas * asientosPorFila` para `NUMERADO`, pero
      `SectorRequest` todavía expone `capacidadMax` y el validador la acepta
      cuando coincide.
- [x] Hacer que `NUMERADO` **no acepte `capacidadMax` en el request**:
      rechazarlo con 400 o modelar un request que no la exponga. No alcanza
      con ignorarla en el mapper.
- [x] Hacer `filas` y `asientosPorFila` obligatorios y mayores a cero en
      `NUMERADO`.
- [x] Agregar test que verifique rechazo de `capacidadMax` informada en
      `NUMERADO` y persistencia de 200 para `filas = 10`,
      `asientosPorFila = 20`.
- [x] Commitear esta corrección **en un commit separado** del resto del
      reporte, con su test.
- [x] Confirmar que `EventoSector` no guarda capacidad y documentar que la
      capacidad es la física del sector, provista por
      `SectorResponse.capacidadMax`.

## msvc-entrada

- [x] Crear `ReporteVentasEventoResponse`, `ReporteSectorResponse` y
      `TotalesReporteResponse` con `entradasEmitidas`, `entradasActivas`,
      `entradasDevueltas`, `enReintegro` y los importes del diseño.
- [x] Agregar consultas de entradas por estado e `idSector` **agregadas con
      `GROUP BY (idSector, estado)`**, no una consulta por estado.
- [x] Agregar consultas de dinero **agregadas con
      `GROUP BY (idSector, estadoPago)`**, sumando
      `RegistroPago.montoTotal` mediante `Entrada -> RegistroPago -> Pago`.
- [x] Combinar ambos agrupamientos por `idSector` en el service.
- [x] Aplicar las reglas: emitidas = `VALIDA|USADA|EN_REINTEGRO|DEVUELTA`;
      activas = `VALIDA|USADA`; devueltas = `DEVUELTA`;
      enReintegro = `EN_REINTEGRO`.
- [x] Aplicar las reglas de pago: bruta = `APROBADO|REINTEGRO_PENDIENTE|
      REEMBOLSADO`; devuelta = `REEMBOLSADO`; neta = bruta - devuelta.
- [x] Verificar que el cálculo sume un `RegistroPago` por entrada, sin
      duplicar montos; el modelo actual tiene `Pago.idEntrada` y clave
      `(idEntrada, idPago)`.
- [x] Crear `ReporteVentasService` con las reglas de conteo, reintegro y
      recaudación neta.
- [x] Aplicar redondeo `HALF_UP` a 2 decimales en todos los importes.
- [x] Devolver `porSector` vacío y totales en 0 cuando no haya entradas.
- [x] Exponer `GET /internal/entradas/eventos/{idEvento}/reporte` sin ruta
      pública en el gateway.
- [x] Verificar que `/internal/**` siga denegado en el gateway.

## msvc-evento

- [x] Crear `ReporteEventoResponse` extendiendo el contrato interno con
      `estadoEvento`, `nombreSector`, `capacidad` y `porcentajeOcupacion`.
- [x] Crear `ReporteOrganizadorService` con validación de pertenencia,
      consulta a `SectorClient` y consumo de `EntradaClient`.
- [x] Calcular porcentaje por sector y total con `entradasActivas` y
      `SectorResponse.capacidadMax`, `HALF_UP` a 2 decimales y 0.00 ante
      capacidad nula o 0.
- [x] Tratar el 404 de `SectorClient` como sector borrado sin FK: incluir el
      item con `capacidad = 0`, `nombreSector = null`,
      `porcentajeOcupacion = 0.00` y cifras intactas.
- [x] Tratar error de red o 5xx de `SectorClient` como 502, sin cifras
      parciales. No confundir 404 con 5xx.
- [x] Responder 403 si el evento existe pero pertenece a otro organizador y
      404 si no existe; la razón ya está documentada en la spec, sin cifras en
      el 403.
- [x] Mapear `idEvento` no numérico a 400.
- [x] Mapear `X-User-Id` ausente o no numérico a 403, nunca 500 por
      `NumberFormatException`. Convertir el header a `String` y validarlo
      antes de parsear, o mapear las excepciones de Spring a 403 para ese
      header.
- [x] Devolver 200 con el snapshot final para un evento cancelado, con
      `estadoEvento = "CANCELADO"` y descuento solo de pagos `REEMBOLSADO`.
- [x] Mapear fallo de `EntradaClient` o body incompleto a 502.
- [x] Agregar `GET /{idEvento}/reporte` a `OrganizadorEventoController`
      leyendo `X-User-Id` de forma segura.
- [x] Actualizar `EntradaClient` al contrato interno nuevo.
- [x] Ajustar `cantidadVendida()` para usar
      `totales.entradasActivas` del reporte completo, no las emitidas
      históricas ni el path viejo.
- [x] Registrar el riesgo de rendimiento de `cantidadVendida()`: una llamada
      con join a pagos por evento desde la búsqueda por relevancia.
- [x] No agregar consultas de roles contra `UsuarioClient`.

## Gateway

- [x] Confirmar que `organizador-eventos-route` con
      `Path=/api/organizador/eventos/**` ya enruta al reporte hacia
      `lb://msvc-evento` en `application.yml` y en la configuración central.
- [x] No agregar `organizador-reportes-route`.
- [x] Confirmar que `GatewayRoleMatrix` cubre el prefijo sin agregar una regla
      nueva.
- [x] Agregar en `GatewayRoleMatrixTest` solo casos de rol del reporte:
      `ORGANIZADOR` permitido y `CLIENTE`, `ADMINISTRADOR` y
      `PERSONAL_ACCESO` denegados.
- [x] Especificar y probar `/internal/**`: 401 sin token y 403 con token,
      ambos con el formato uniforme `{status, error, message, path}`.
- [x] Agregar el caso sin token en `GatewayErrorHandlingTest`; el caso con
      token ya existe en `GatewayRoleMatrixTest`
      (`contrato interno siempre denegado`).
- [x] No mover ni duplicar en `GatewayRoleMatrixTest` los casos de
      sobrescritura de `X-User-Id` y descarte de `X-User-Role`: ya están en
      `GatewayErrorHandlingTest`.

## Tests unitarios y de gateway

- [x] `msvc-entrada`: emitidas, activas, usadas, reservadas, en reintegro,
      devueltas, bruta, devuelta, neta, doble descuento y sin movimientos.
- [x] `msvc-entrada`: dos entradas aprobadas para el mismo usuario y evento
      suman dos `RegistroPago` y la recaudación es la suma, sin duplicar un
      pago compartido.
- [x] `msvc-entrada`: verificar que la agregación usa `GROUP BY` y no una
      consulta por estado.
- [x] `msvc-entrada`: pago `REEMBOLSADO` en `recaudacionDevuelta` y no en
      `recaudacionNeta`; el mock con `APRUEBA` ya permite ese estado y
      `PagoServiceTest` cubre la transición baja.
- [x] `msvc-evento`: sector `NUMERADO` con `filas = 10` y
      `asientosPorFila = 20`: `capacidadMax = 200` y 120 activas dan
      `porcentajeOcupacion = 60.00`. El reporte consume la capacidad ya
      calculada por `msvc-ubicacion`, no recalcula el producto.
- [x] `msvc-evento`: `SectorClient` 404 → item con capacidad 0, nombre nulo,
      ocupación 0.00 y cifras intactas.
- [x] `msvc-evento`: `SectorClient` 5xx o error de red → 502 sin cifras
      parciales.
- [x] `msvc-evento`: dueño, otro organizador (403), inexistente (404),
      `idEvento` no numérico (400), `X-User-Id` ausente (403) y no numérico
      (403), sin `NumberFormatException`.
- [x] `msvc-evento`: invariante `suma(porSector.campo) == totales.campo` para
      todos los conteos y los tres importes.
- [x] `msvc-evento`: aislamiento entre eventos; el reporte del evento A no
      incluye entradas del evento B.
- [x] `msvc-evento`: evento cancelado con mock en `FALLA` → entradas
      `EN_REINTEGRO`, pagos `REINTEGRO_PENDIENTE`,
      `recaudacionDevuelta = 0` y neta completa.
- [x] `msvc-evento`: regla `DEVUELTA + REINTEGRO_PENDIENTE` con **datos
      armados a mano**. El nombre del test y un comentario deben aclarar que
      es un fixtures artificial: con `FALLA` el flujo real deja la entrada en
      `EN_REINTEGRO`, así que ese estado no se alcanza por el mock.
- [x] Agregar tests nuevos de `cantidadVendida()` con el contrato interno
      nuevo. No existen hoy tests que referencien el path viejo; si apareciera
      alguno, se reemplaza en commit separado sin relajar la aserción y se
      registra acá.
- [x] `msvc-ubicacion`: rechazo de `capacidadMax` en `NUMERADO` y cálculo
      servidor de `filas * asientosPorFila` (commit propio).
- [x] Gateway: `/internal/**` sin token → 401 y con token → 403, con el
      formato uniforme.

## Verificación y archivado

- [x] Ejecutar `mvn -pl msvc-ubicacion test`, `-pl msvc-entrada test`,
      `-pl msvc-evento test`, `-pl msvc-gateway test` y `mvn -B package`
      completos.
- [x] Confirmar que el build sigue pasando con `NoDobleVentaTest` intacto.
- [x] **No ejecutar la verificación de integración con docker-compose en este
      change**: se mueve a EP-21 según la decisión registrada en `design.md`.
- [ ] Crear/actualizar la tarea de seguimiento en EP-21
      (`verificacion-e2e-carga`) que referencie `reportes-organizador` y
      ejecute el flujo del reporte con el compose levantado, sin Testcontainers
      nuevo. **Pendiente:** el change `verificacion-e2e-carga` todavía no existe;
      requiere su propia propuesta y no se crea dentro de este change.
- [x] Archivar este change **al pasar los tests unitarios y de gateway**, sin
      esperar a EP-21.

## Cierre OpenSpec

- [x] Publicar `reportes-organizador` en `openspec/specs/`.
- [x] Publicar los cambios de `gateway-ruteo`, `gateway-seguridad` e
      `identidad-servicios` en sus specs vivas.
- [x] Actualizar `openspec/config.yaml`: alinear el contrato interno del
      reporte, marcar EP-18 como cubierto y quitar el punto abierto de la
      credencial de login, ya resuelto con email, BCrypt y JWT.
- [x] No agregar ADR: no hay tecnología ni patrón nuevo.
- [x] Archivar el change moviendo la carpeta a
      `changes/archive/2026-11-22-reportes-organizador`. Por decisión del
      usuario no se toca `.openspec.yaml`, por lo que no se agrega la fecha
      `archived` en ese archivo.
- [x] No hacer push sin autorización explícita.

## Registro de commits separados

- [x] Commit 1: corrección de `msvc-ubicacion` para sectores `NUMERADO`, con
      su test.
- [x] Commit 2: implementación del contrato interno y la agregación en
      `msvc-entrada`.
- [x] Commit 3: implementación del reporte en `msvc-evento` y ajuste de
      `cantidadVendida()`.
- [x] Commit 4: casos de gateway (roles y `/internal/**`).
- [x] Commit 5 (solo si hace falta): reemplazo de tests obsoletos por el
      cambio de contrato de `cantidadVendida()`, sin relajar aserciones.
      No hizo falta: no existían tests que referenciaran el path viejo. En su
      lugar se agregó `EventoServiceImplRelevanciaTest` con el contrato nuevo.
- [x] Commit 6: cierre documental y archivado OpenSpec.
