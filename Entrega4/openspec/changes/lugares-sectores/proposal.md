# Proposal

## Why

`msvc-ubicacion` solo tiene el esqueleto del servicio y aún no modela los lugares ni su estructura de sectores. Evento necesita consultar el lugar (ciudad y ubicación) y el sector (existencia y pertenencia al lugar) para configurar y publicar eventos, y Entrada necesita la capacidad y la configuración de asientos para armar la disponibilidad. Sin este servicio, esos contratos no tienen quien los responda.

## What Changes

- Modelar `Lugar` (nombre, dirección, ciudad, latitud y longitud) y `Sector` con tipo `NUMERADO` o `NO_NUMERADO`.
- En sectores numerados, guardar filas y asientos por fila y calcular la capacidad como filas por asientos por fila; en no numerados, exigir una capacidad máxima mayor a cero.
- Crear un lugar junto con sus sectores, consultarlo con sus sectores, listarlo (por ciudad o por ids), editar sus datos y agregar sectores a un lugar existente.
- Exponer `GET /api/lugares/{id}` y `GET /api/sectores/{id}` con los contratos que ya consumen Evento y Entrada.
- Rechazar con 400 y el detalle de cada error los lugares sin sectores, las coordenadas inválidas y los sectores incoherentes.

**Fuera de alcance:** precios por sector y evento (viven en `msvc-evento`), disponibilidad, reservas y asientos ocupados (viven en `msvc-entrada`), edición o baja de sectores ya creados, baja de lugares, autenticación y autorización, integración con un servicio de mapas externo, caché y cualquier funcionalidad exclusiva de Entrega 2.

## Capabilities

### New Capabilities
- `lugares-sectores`: alta, consulta y edición de lugares y de la estructura física de sus sectores.

### Modified Capabilities
- Ninguna.

## Impact

- **Servicio:** `msvc-ubicacion` (entidades, capas de servicio y API REST, persistencia PostgreSQL `ticketya_ubicacion`, pruebas y documentación).
- **Contratos:** `GET /api/lugares/{id}` (Evento) y `GET /api/sectores/{id}` (Evento y Entrada); los demás servicios guardan `id_lugar` e `id_sector` sin clave foránea, por lo que los ids deben ser estables.
- **Modelo:** tablas `lugar` y `sector`, con unicidad del nombre de sector dentro de un lugar.
- **Seguridad:** sin autenticación en Entrega 1; no se agregan dependencias.
