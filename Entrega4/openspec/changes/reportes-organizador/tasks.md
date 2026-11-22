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
- [ ] **Condición (a):** no iniciar la implementación hasta que el smoke test
      de login real esté verde: emisión RS256 en `msvc-usuarios`, validación
      en el gateway y llegada de `X-User-Id` al servicio destino.
- [ ] **Condición (b):** no iniciar la implementación hasta que esté integrado
      el commit de `msvc-evento` de separación de paths por rol, que toca
      `organizador-eventos-route` y la eliminación de `validarRol`.

## Corrección previa en msvc-ubicacion (commit propio)

- [ ] Verificar el estado actual: `UbicacionMapper` ya calcula
      `capacidadMax = filas * asientosPorFila` para `NUMERADO`, pero
      `SectorRequest` todavía expone `capacidadMax` y el validador la acepta
      cuando coincide.
- [ ] Hacer que `NUMERADO` **no acepte `capacidadMax` en el request**:
      rechazarlo con 400 o modelar un request que no la exponga. No alcanza
      con ignorarla en el mapper.
- [ ] Hacer `filas` y `asientosPorFila` obligatorios y mayores a cero en
      `NUMERADO`.
- [ ] Agregar test que verifique rechazo de `capacidadMax` informada en
      `NUMERADO` y persistencia de 200 para `filas = 10`,
      `asientosPorFila = 20`.
- [ ] Commitear esta corrección **en un commit separado** del resto del
      reporte, con su test.
- [ ] Confirmar que `EventoSector` no guarda capacidad y documentar que la
      capacidad es la física del sector, provista por
      `SectorResponse.capacidadMax`.

## msvc-entrada

- [ ] Crear `ReporteVentasEventoResponse`, `ReporteSectorResponse` y
      `TotalesReporteResponse` con `entradasEmitidas`, `entradasActivas`,
      `entradasDevueltas`, `enReintegro` y los importes del diseño.
- [ ] Agregar consultas de entradas por estado e `idSector` **agregadas con
      `GROUP BY (idSector, estado)`**, no una consulta por estado.
- [ ] Agregar consultas de dinero **agregadas con
      `GROUP BY (idSector, estadoPago)`**, sumando
      `RegistroPago.montoTotal` mediante `Entrada -> RegistroPago -> Pago`.
- [ ] Combinar ambos agrupamientos por `idSector` en el service.
- [ ] Aplicar las reglas: emitidas = `VALIDA|USADA|EN_REINTEGRO|DEVUELTA`;
      activas = `VALIDA|USADA`; devueltas = `DEVUELTA`;
      enReintegro = `EN_REINTEGRO`.
- [ ] Aplicar las reglas de pago: bruta = `APROBADO|REINTEGRO_PENDIENTE|
      REEMBOLSADO`; devuelta = `REEMBOLSADO`; neta = bruta - devuelta.
- [ ] Verificar que el cálculo sume un `RegistroPago` por entrada, sin
      duplicar montos; el modelo actual tiene `Pago.idEntrada` y clave
      `(idEntrada, idPago)`.
- [ ] Crear `ReporteVentasService` con las reglas de conteo, reintegro y
      recaudación neta.
- [ ] Aplicar redondeo `HALF_UP` a 2 decimales en todos los importes.
- [ ] Devolver `porSector` vacío y totales en 0 cuando no haya entradas.
- [ ] Exponer `GET /internal/entradas/eventos/{idEvento}/reporte` sin ruta
      pública en el gateway.
- [ ] Verificar que `/internal/**` siga denegado en el gateway.

## msvc-evento

- [ ] Crear `ReporteEventoResponse` extendiendo el contrato interno con
      `estadoEvento`, `nombreSector`, `capacidad` y `porcentajeOcupacion`.
- [ ] Crear `ReporteOrganizadorService` con validación de pertenencia,
      consulta a `SectorClient` y consumo de `EntradaClient`.
- [ ] Calcular porcentaje por sector y total con `entradasActivas` y
      `SectorResponse.capacidadMax`, `HALF_UP` a 2 decimales y 0.00 ante
      capacidad nula o 0.
- [ ] Tratar el 404 de `SectorClient` como sector borrado sin FK: incluir el
      item con `capacidad = 0`, `nombreSector = null`,
      `porcentajeOcupacion = 0.00` y cifras intactas.
- [ ] Tratar error de red o 5xx de `SectorClient` como 502, sin cifras
      parciales. No confundir 404 con 5xx.
- [ ] Responder 403 si el evento existe pero pertenece a otro organizador y
      404 si no existe; la razón ya está documentada en la spec, sin cifras en
      el 403.
- [ ] Mapear `idEvento` no numérico a 400.
- [ ] Mapear `X-User-Id` ausente o no numérico a 403, nunca 500 por
      `NumberFormatException`. Convertir el header a `String` y validarlo
      antes de parsear, o mapear las excepciones de Spring a 403 para ese
      header.
- [ ] Devolver 200 con el snapshot final para un evento cancelado, con
      `estadoEvento = "CANCELADO"` y descuento solo de pagos `REEMBOLSADO`.
- [ ] Mapear fallo de `EntradaClient` o body incompleto a 502.
- [ ] Agregar `GET /{idEvento}/reporte` a `OrganizadorEventoController`
      leyendo `X-User-Id` de forma segura.
- [ ] Actualizar `EntradaClient` al contrato interno nuevo.
- [ ] Ajustar `cantidadVendida()` para usar
      `totales.entradasActivas` del reporte completo, no las emitidas
      históricas ni el path viejo.
- [ ] Registrar el riesgo de rendimiento de `cantidadVendida()`: una llamada
      con join a pagos por evento desde la búsqueda por relevancia.
- [ ] No agregar consultas de roles contra `UsuarioClient`.

## Gateway

- [ ] Confirmar que `organizador-eventos-route` con
      `Path=/api/organizador/eventos/**` ya enruta al reporte hacia
      `lb://msvc-evento` en `application.yml` y en la configuración central.
- [ ] No agregar `organizador-reportes-route`.
- [ ] Confirmar que `GatewayRoleMatrix` cubre el prefijo sin agregar una regla
      nueva.
- [ ] Agregar en `GatewayRoleMatrixTest` solo casos de rol del reporte:
      `ORGANIZADOR` permitido y `CLIENTE`, `ADMINISTRADOR` y
      `PERSONAL_ACCESO` denegados.
- [ ] Especificar y probar `/internal/**`: 401 sin token y 403 con token,
      ambos con el formato uniforme `{status, error, message, path}`.
- [ ] Agregar el caso sin token en `GatewayErrorHandlingTest`; el caso con
      token ya existe en `GatewayRoleMatrixTest`
      (`contrato interno siempre denegado`).
- [ ] No mover ni duplicar en `GatewayRoleMatrixTest` los casos de
      sobrescritura de `X-User-Id` y descarte de `X-User-Role`: ya están en
      `GatewayErrorHandlingTest`.

## Tests unitarios y de gateway

- [ ] `msvc-entrada`: emitidas, activas, usadas, reservadas, en reintegro,
      devueltas, bruta, devuelta, neta, doble descuento y sin movimientos.
- [ ] `msvc-entrada`: dos entradas aprobadas para el mismo usuario y evento
      suman dos `RegistroPago` y la recaudación es la suma, sin duplicar un
      pago compartido.
- [ ] `msvc-entrada`: verificar que la agregación usa `GROUP BY` y no una
      consulta por estado.
- [ ] `msvc-entrada`: pago `REEMBOLSADO` en `recaudacionDevuelta` y no en
      `recaudacionNeta`; el mock con `APRUEBA` ya permite ese estado y
      `PagoServiceTest` cubre la transición baja.
- [ ] `msvc-evento`: sector `NUMERADO` con `filas = 10` y
      `asientosPorFila = 20`: `capacidadMax = 200` y 120 activas dan
      `porcentajeOcupacion = 60.00`. El reporte consume la capacidad ya
      calculada por `msvc-ubicacion`, no recalcula el producto.
- [ ] `msvc-evento`: `SectorClient` 404 → item con capacidad 0, nombre nulo,
      ocupación 0.00 y cifras intactas.
- [ ] `msvc-evento`: `SectorClient` 5xx o error de red → 502 sin cifras
      parciales.
- [ ] `msvc-evento`: dueño, otro organizador (403), inexistente (404),
      `idEvento` no numérico (400), `X-User-Id` ausente (403) y no numérico
      (403), sin `NumberFormatException`.
- [ ] `msvc-evento`: invariante `suma(porSector.campo) == totales.campo` para
      todos los conteos y los tres importes.
- [ ] `msvc-evento`: aislamiento entre eventos; el reporte del evento A no
      incluye entradas del evento B.
- [ ] `msvc-evento`: evento cancelado con mock en `FALLA` → entradas
      `EN_REINTEGRO`, pagos `REINTEGRO_PENDIENTE`,
      `recaudacionDevuelta = 0` y neta completa.
- [ ] `msvc-evento`: regla `DEVUELTA + REINTEGRO_PENDIENTE` con **datos
      armados a mano**. El nombre del test y un comentario deben aclarar que
      es un fixtures artificial: con `FALLA` el flujo real deja la entrada en
      `EN_REINTEGRO`, así que ese estado no se alcanza por el mock.
- [ ] Agregar tests nuevos de `cantidadVendida()` con el contrato interno
      nuevo. No existen hoy tests que referencien el path viejo; si apareciera
      alguno, se reemplaza en commit separado sin relajar la aserción y se
      registra acá.
- [ ] `msvc-ubicacion`: rechazo de `capacidadMax` en `NUMERADO` y cálculo
      servidor de `filas * asientosPorFila` (commit propio).
- [ ] Gateway: `/internal/**` sin token → 401 y con token → 403, con el
      formato uniforme.

## Verificación y archivado

- [ ] Ejecutar `mvn -pl msvc-ubicacion test`, `-pl msvc-entrada test`,
      `-pl msvc-evento test`, `-pl msvc-gateway test` y `mvn -B package`
      completos.
- [ ] Confirmar que el build sigue pasando con `NoDobleVentaTest` intacto.
- [ ] **No ejecutar la verificación de integración con docker-compose en este
      change**: se mueve a EP-21 según la decisión registrada en `design.md`.
- [ ] Crear/actualizar la tarea de seguimiento en EP-21
      (`verificacion-e2e-carga`) que referencie `reportes-organizador` y
      ejecute el flujo del reporte con el compose levantado, sin Testcontainers
      nuevo.
- [ ] Archivar este change **al pasar los tests unitarios y de gateway**, sin
      esperar a EP-21.

## Cierre OpenSpec

- [ ] Publicar `reportes-organizador` en `openspec/specs/`.
- [ ] Publicar los cambios de `gateway-ruteo`, `gateway-seguridad` e
      `identidad-servicios` en sus specs vivas.
- [ ] Actualizar `openspec/config.yaml`: alinear el contrato interno del
      reporte, marcar EP-18 como cubierto y quitar el punto abierto de la
      credencial de login, ya resuelto con email, BCrypt y JWT.
- [ ] No agregar ADR: no hay tecnología ni patrón nuevo.
- [ ] Archivar el change con `.openspec.yaml` → `archived: 2026-11-22`.
- [ ] No hacer push sin autorización explícita.

## Registro de commits separados

- [ ] Commit 1: corrección de `msvc-ubicacion` para sectores `NUMERADO`, con
      su test.
- [ ] Commit 2: implementación del contrato interno y la agregación en
      `msvc-entrada`.
- [ ] Commit 3: implementación del reporte en `msvc-evento` y ajuste de
      `cantidadVendida()`.
- [ ] Commit 4: casos de gateway (roles y `/internal/**`).
- [ ] Commit 5 (solo si hace falta): reemplazo de tests obsoletos por el
      cambio de contrato de `cantidadVendida()`, sin relajar aserciones.
- [ ] Commit 6: cierre documental y archivado OpenSpec.
