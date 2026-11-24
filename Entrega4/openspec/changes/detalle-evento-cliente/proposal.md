# Proposal: detalle de evento para el cliente

## Por qué
El frontend necesita mostrar la página de un evento con su lugar (nombre, dirección, ciudad) y sus sectores (nombre, tipo, capacidad, precio). Hoy `GET /api/eventos/{id}` solo devuelve `idLugar` y `[{idSector, precio}]`, y los endpoints de msvc-ubicacion (`/api/lugares`, `/api/sectores`) están restringidos a ADMINISTRADOR y ORGANIZADOR, por lo que un cliente no puede resolver esos datos.

## Qué cambia
- msvc-evento: nuevo `GET /api/eventos/{id}/detalle` (público, igual que el resto de las consultas de eventos) que compone el evento con el lugar y sus sectores consultados a msvc-ubicacion.
- `EventoResponse` y los endpoints existentes no cambian.
- Los DTOs internos `LugarResponse` y `SectorResponse` de msvc-evento suman campos opcionales (`sectores`, `tipo`, `filas`, `asientosPorFila`) con constructores previos conservados.

## Fuera de alcance
- Mapa de asientos y listado de asientos ocupados: la disponibilidad se sigue consultando por sector con `/api/eventos/{id}/disponibilidad`.
- Cambios en msvc-ubicacion o en el gateway (la ruta `GET /api/eventos/**` ya es pública).

## Riesgos
- Si msvc-ubicacion no responde, el detalle falla con el error de servicio externo ya existente (502/503 según el handler).
