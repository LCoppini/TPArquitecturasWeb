# Tareas

## Revisión previa

- [x] Revisar proposal, design y specs EARS con el grupo y aprobarlos antes de
      escribir código.
- [x] Confirmar las tres decisiones propuestas: `EN_REINTEGRO` no ocupa,
      sector sin capacidad se incluye con 0, y
      `totales.porcentajeOcupacion` se calcula sobre capacidad total.
- [x] Confirmar que no se modificarán tests existentes para que pasen, que no
      se tocará `NoDobleVentaTest`, que no se hará push y que no se agregará
      ADR.

## msvc-entrada

- [ ] Crear `ReporteVentasEventoResponse`, `ReporteSectorResponse` y
      `TotalesReporteResponse` con los campos y tipos del diseño.
- [ ] Agregar consultas de entradas por estado e `idSector`.
- [ ] Agregar consultas de `RegistroPago.montoTotal` por estado de pago,
      uniendo `Entrada -> RegistroPago -> Pago`.
- [ ] Crear `ReporteVentasService` con las reglas de conteo, reintegro y
      recaudación neta.
- [ ] Aplicar redondeo `HALF_UP` a 2 decimales en todos los importes.
- [ ] Devolver `porSector` vacío y totales en 0 cuando no haya entradas.
- [ ] Exponer `GET /internal/entradas/eventos/{idEvento}/reporte` sin ruta
      pública en el gateway.
- [ ] Verificar que `/internal/**` siga denegado en el gateway.

## msvc-evento

- [ ] Crear `ReporteEventoResponse` extendiendo el contrato interno con
      `nombreSector`, `capacidad` y `porcentajeOcupacion`.
- [ ] Crear `ReporteOrganizadorService` con validación de pertenencia,
      consulta a `SectorClient` y consumo de `EntradaClient`.
- [ ] Calcular porcentaje por sector y total con `capacidadMax`, `HALF_UP` a
      2 decimales y 0.00 ante capacidad nula o 0.
- [ ] Incluir sectores con ventas pero sin capacidad configurada usando
      `capacidad = 0`.
- [ ] Mapear pertenencia inválida a 403, evento inexistente a 404, id no
      numérico a 400 y fallo de Feign o body incompleto a 502.
- [ ] Agregar `GET /{idEvento}/reporte` a `OrganizadorEventoController`
      leyendo `X-User-Id`.
- [ ] Actualizar `EntradaClient` al contrato interno nuevo.
- [ ] Ajustar `cantidadVendida()` para usar `totales.entradasVendidas`.
- [ ] No agregar consultas de roles contra `UsuarioClient`.

## Gateway

- [ ] Agregar `organizador-reportes-route` en
      `msvc-gateway/src/main/resources/application.yml` y en
      `msvc-config/src/main/resources/configuration/msvc-gateway.yml`.
- [ ] Confirmar que `GatewayRoleMatrix` cubre el prefijo sin agregar una regla
      nueva.
- [ ] Agregar en `GatewayRoleMatrixTest`: organizador permitido, roles
      denegados, sobrescritura de `X-User-Id` y descarte de `X-User-Role`.
- [ ] Verificar que la ruta aparece en ambos archivos YAML.

## Verificación

- [ ] Tests unitarios de `msvc-entrada`: conteos por estado, usadas,
      reservadas, en reintegro, devueltas, bruta, devuelta, neta,
      doble descuento y sin movimientos.
- [ ] Test de integración de `msvc-entrada` con Testcontainers.
- [ ] Tests unitarios de `msvc-evento`: dueño, otro organizador,
      inexistente, identidad ausente, Feign caído, body incompleto,
      `SectorClient` caído, ocupación, capacidad 0, sector sin ventas y
      sector sin capacidad.
- [ ] Ejecutar `mvn -pl msvc-entrada test`, `-pl msvc-evento test`,
      `-pl msvc-gateway test` y `mvn -B package` completos.
- [ ] Confirmar que el build sigue pasando con `NoDobleVentaTest` intacto.

## Cierre OpenSpec

- [ ] Publicar `reportes-organizador` en `openspec/specs/`.
- [ ] Publicar los cambios de `gateway-ruteo`, `gateway-seguridad` e
      `identidad-servicios` en sus specs vivas.
- [ ] Actualizar `openspec/config.yaml` para alinear el contrato interno y
      marcar EP-18 como cubierto.
- [ ] No agregar ADR: no hay tecnología ni patrón nuevo.
- [ ] Archivar el change con `.openspec.yaml` → `archived: 2026-11-21`.
- [ ] No hacer push sin autorización explícita.
