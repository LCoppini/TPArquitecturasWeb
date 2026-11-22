# Bitácora: Tema 3 (roles y gateway) — corrección de registro

## Registro anterior (desactualizado)

En el cierre del tema 3 se había anotado:

> `msvc-gateway`: sin cambios de código, solo casos de test nuevos.

Ese registro asumía que un único path compartido (`/api/preferencias/**` y
`/api/recomendaciones/**`) con `hasAnyRole(ADMINISTRADOR, CLIENTE)` alcanzaba
para que el servicio distinguiera al llamante y decidiera si aplicaba la
comparación de pertenencia.

## Defecto detectado durante el análisis

ADR-0001 elimina `X-User-Role` en el gateway y los servicios reciben únicamente
`X-User-Id`. Bajo un path compartido, `msvc-preferencias` no tiene forma de saber
si el llamante es `CLIENTE` o `ADMINISTRADOR`; por lo tanto, no puede decidir
cuándo comparar el `{idUsuario}` del path contra `X-User-Id`. El esquema de path
compartido era irrealizable sin romper ADR-0001.

## Corrección (ya reflejada en `design.md` y en las specs delta)

Se separa la autorización por path, sin propagar roles al servicio:

- `GET /api/preferencias/clientes/{idUsuario}` → rol `CLIENTE`; el servicio
  compara el path contra `X-User-Id` y responde 403 si difieren.
- `GET /api/admin/preferencias/clientes/{idUsuario}` → rol `ADMINISTRADOR`; el
  servicio no aplica la comparación.
- El mismo patrón para `/api/recomendaciones/clientes/{idUsuario}` y
  `/api/admin/recomendaciones/clientes/{idUsuario}`.

La decisión funcional original no cambia: `CLIENTE` consulta solo su id y
`ADMINISTRADOR` consulta cualquier id. Lo que cambia es el mecanismo: el rol se
resuelve en el gateway según el path, en lugar de intentar reconstruirlo dentro
del servicio.

## Impacto en `msvc-gateway` (ya planificado en `tasks.md` 6.1-6.3)

Sí hay cambios de código en el gateway:

- Agregar `admin-preferencias-route` y `admin-recomendaciones-route` hacia
  `lb://msvc-preferencias`.
- Mover `/api/preferencias/**` y `/api/recomendaciones/**` al array `CLIENTE`.
- Incorporar `/api/admin/preferencias/**` y `/api/admin/recomendaciones/**` al
  array `ADMIN`.
- Retirar `CLIENTE_Y_ADMIN` y actualizar `prefijosConRegla()`.
- Sumar los casos de test correspondientes en `GatewayRoleMatrixTest`.

## Naturaleza del ajuste

Esto **corrige un defecto de diseño detectado durante el análisis**; no es un
cambio de alcance solicitado ni una funcionalidad nueva. Se deja constancia para
que la bitácora del tema 3 sea coherente con lo ya documentado en `design.md`,
las specs delta y `tasks.md`.
