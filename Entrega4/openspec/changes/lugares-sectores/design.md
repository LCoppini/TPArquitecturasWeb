# Design

## Decisions

1. **Capas como el resto de los servicios.** `controller` → `service` (interfaz + `impl`) → `repository`/`entity`, con DTOs `request` y `response`, un `mapper` manual y un manejador de excepciones propio con el formato de error `{fecha, estado, error, mensaje, ruta}` que ya usa Evento.
2. **Sin precio ni disponibilidad.** El precio pertenece a la combinación evento + sector y vive en `msvc-evento`; la disponibilidad vive en `msvc-entrada`. `Sector` guarda solo la estructura física.
3. **Capacidad calculada.** En un sector numerado la capacidad es `filas × asientosPorFila` y se persiste en `capacidad_max`; si el cliente la informa y no coincide, se rechaza. En un sector no numerado se exige `capacidadMax` y no se admiten filas ni asientos.
4. **Contratos compatibles con los consumidores.** `LugarResponse` devuelve `id`, `nombre`, `direccion`, `ciudad` y `ubicacion` (`"latitud,longitud"`), más `latitud`, `longitud` y `sectores`. `SectorResponse` devuelve `idSector`, `idLugar`, `nombre`, `tipo`, `filas`, `asientosPorFila` y `capacidadMax`, con los nombres que esperan los clientes Feign de Evento y Entrada.
5. **Validación en dos niveles.** Bean Validation cubre campos obligatorios, rangos de coordenadas, lista de sectores no vacía y valores positivos; el servicio cubre la coherencia entre tipo y capacidad y los nombres repetidos, y acumula todos los errores en un único 400.
6. **Ids estables.** Los sectores no se editan ni se eliminan en Entrega 1; solo se pueden agregar sectores nuevos a un lugar. Así los `id_sector` guardados por otros servicios no quedan inválidos.
7. **Consulta en bloque.** `GET /api/lugares?ids=1,2,3` permite a otros servicios resolver varios lugares con una llamada; `?ciudad=` filtra sin distinguir mayúsculas.

## Decisiones pendientes (no son requisitos)

- **Baja de sectores y lugares.** La épica pide que no se pueda borrar un sector referenciado por un evento. Como no hay clave foránea entre servicios, hace falta que Evento exponga si un sector está en uso (por ejemplo, un endpoint de consulta) o que Ubicación no permita borrar. Hasta que el grupo lo decida, Ubicación no expone operaciones de baja.
- **Mapa externo.** El uso opcional de un servicio de mapas para validar o completar coordenadas queda fuera de esta entrega.

## Risks / Trade-offs

- **Sin edición de sectores:** corregir un error de carga (por ejemplo, la capacidad) requiere una nueva decisión del grupo, porque editarlos podría invalidar reservas o configuraciones ya hechas por otros servicios.
- **`ddl-auto: update`:** el esquema lo genera Hibernate solo para desarrollo, igual que en los demás servicios; la unicidad de `(id_lugar, nombre)` queda como restricción de tabla.
- **Dependencia de datos cargados:** para probar el flujo completo, Evento y Entrada necesitan que existan lugares y sectores creados por esta API.
