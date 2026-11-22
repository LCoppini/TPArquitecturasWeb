# Tareas

## Revisión previa y dependencias

- [x] Revisar proposal, design y specs EARS con el grupo y aprobarlos antes de
      escribir código.
- [x] Confirmar las tres decisiones propuestas: `EN_REINTEGRO` no ocupa,
      sector sin capacidad se incluye con 0, y
      `totales.porcentajeOcupacion` se calcula sobre capacidad total.
- [x] Confirmar que no se modificarán tests existentes para que pasen, que no
      se tocará `NoDobleVentaTest`, que no se hará push y que no se agregará
      ADR.
- [x] Aplicar los ajustes aprobados: `entradasEmitidas` vs `entradasActivas`,
      `RegistroPago.montoTotal` por entrada, mock `REEMBOLSADO`, identidad en
      `GatewayErrorHandlingTest`, ruta genérica sin ruta nueva, 403/404,
      integración con docker-compose y JSON coherente.
- [ ] No iniciar la implementación hasta que el smoke test de login real esté
      verde: emisión RS256 en `msvc-usuarios`, validación en el gateway y
      llegada de `X-User-Id` al servicio destino.
- [ ] Confirmar que el entorno de `docker-compose` tiene los servicios
      levantados antes de ejecutar la prueba de integración; este change no
      implementa EP-21.

## msvc-entrada

- [ ] Crear `ReporteVentasEventoResponse`, `ReporteSectorResponse` y
      `TotalesReporteResponse` con `entradasEmitidas`, `entradasActivas`,
      `entradasDevueltas`, `enReintegro` y los importes del diseño.
- [ ] Agregar consultas de entradas por estado e `idSector`.
- [ ] Agrupar con las reglas: emitidas = `VALIDA|USADA|EN_REINTEGRO|DEVUELTA`;
      activas = `VALIDA|USADA`; devueltas = `DEVUELTA`;
      enReintegro = `EN_REINTEGRO`.
- [ ] Agregar consultas de `RegistroPago.montoTotal` por estado de pago,
      uniendo `Entrada -> RegistroPago -> Pago`.
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
      `capacidadMax`, `HALF_UP` a 2 decimales y 0.00 ante capacidad nula o 0.
- [ ] Incluir sectores con entradas emitidas pero sin capacidad configurada
      usando `capacidad = 0`.
- [ ] Responder 403 si el evento existe pero pertenece a otro organizador y
      404 si no existe; documentar la razón en la spec, sin cifras en el 403.
- [ ] Devolver 200 con el snapshot final para un evento cancelado, con
      `estadoEvento = "CANCELADO"`.
- [ ] Mapear id no numérico a 400 y fallo de Feign o body incompleto a 502.
- [ ] Agregar `GET /{idEvento}/reporte` a `OrganizadorEventoController`
      leyendo `X-User-Id`.
- [ ] Actualizar `EntradaClient` al contrato interno nuevo.
- [ ] Ajustar `cantidadVendida()` para usar `totales.entradasActivas`, no las
      emitidas históricas.
- [ ] No agregar consultas de roles contra `UsuarioClient`.
- [ ] No agregar los casos de identidad acá: pertenencia se resuelve por
      `X-User-Id`, no por rol.

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
- [ ] No mover ni duplicar en `GatewayRoleMatrixTest` los casos de
      sobrescritura de `X-User-Id` y descarte de `X-User-Role`: ya están en
      `GatewayErrorHandlingTest`.
- [ ] Si se quiere cobertura específica del path del reporte, agregarla en
      `GatewayErrorHandlingTest`, no en la matriz de roles.

## Tests unitarios

- [ ] `msvc-entrada`: emitidas, activas, usadas, reservadas, en reintegro,
      devueltas, bruta, devuelta, neta, doble descuento y sin movimientos.
- [ ] `msvc-entrada`: dos entradas aprobadas para el mismo usuario y evento
      suman dos `RegistroPago` y la recaudación es la suma, sin duplicar un
      pago compartido.
- [ ] `msvc-entrada`: un pago `REEMBOLSADO` cuenta en `recaudacionDevuelta` y
      no en `recaudacionNeta`; el mock con `APRUEBA` ya permite llegar a ese
      estado y `PagoServiceTest` cubre la transición baja.
- [ ] `msvc-evento`: dueño, otro organizador (403), inexistente (404),
      identidad ausente, Feign caído, body incompleto, `SectorClient` caído,
      ocupación, capacidad 0, sector sin ventas y sector sin capacidad.
- [ ] `msvc-evento`: invariante `suma(porSector.entradas*) == totales.*` para
      todos los campos de conteo y de dinero.
- [ ] `msvc-evento`: aislamiento entre eventos; el reporte del evento A no
      incluye entradas del evento B.
- [ ] `msvc-evento`: evento cancelado responde 200 con `estadoEvento` y
      snapshot final, sin cifras vigentes inventadas.
- [ ] Agregar tests nuevos de `cantidadVendida()` con el contrato interno
      nuevo. No existen hoy tests que referencien el path viejo; si apareciera
      alguno, se reemplaza en commit separado sin relajar la aserción y se
      registra acá.

## Integración con docker-compose

- [ ] Ejecutar la prueba de integración contra el entorno levantado con
      `docker compose up -d`, según ADR-0002, no con Testcontainers nuevo.
- [ ] Verificar el flujo con un organizador dueño y otro ajeno sobre el mismo
      evento, comprobando 200 y 403 a través del gateway.
- [ ] Verificar que los números del reporte coinciden con los datos de
      `msvc-entrada` en el mismo momento, incluida una compra de dos entradas.
- [ ] Verificar el caso de evento cancelado con el compose levantado.
- [ ] Confirmar que la tarea queda pendiente si EP-21 aún no deja los
      servicios en el compose; no se sustituye por Testcontainers.
- [ ] Confirmar que `NoDobleVentaTest` sigue intacto y no se edita para que
      pase.

## Verificación general

- [ ] Ejecutar `mvn -pl msvc-entrada test`, `-pl msvc-evento test`,
      `-pl msvc-gateway test` y `mvn -B package` completos.
- [ ] Confirmar que el build sigue pasando con `NoDobleVentaTest` intacto.

## Cierre OpenSpec

- [ ] Publicar `reportes-organizador` en `openspec/specs/`.
- [ ] Publicar los cambios de `gateway-ruteo`, `gateway-seguridad` e
      `identidad-servicios` en sus specs vivas.
- [ ] Actualizar `openspec/config.yaml`: alinear el contrato interno del
      reporte, marcar EP-18 como cubierto y quitar el punto abierto de la
      credencial de login, ya resuelto con email, BCrypt y JWT.
- [ ] No agregar ADR: no hay tecnología ni patrón nuevo.
- [ ] Archivar el change con `.openspec.yaml` → `archived: 2026-11-21`.
- [ ] No hacer push sin autorización explícita.
