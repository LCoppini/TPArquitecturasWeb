# Tasks

## 1. Persistencia

- [x] 1.1 Modelar las entidades `Lugar` y `Sector` (tipo `NUMERADO`/`NO_NUMERADO`, filas, asientos por fila y capacidad) con unicidad de nombre de sector por lugar; verificar con pruebas que la capacidad de un sector numerado es filas por asientos por fila.
- [x] 1.2 Implementar los repositorios con consultas por ciudad sin distinguir mayúsculas, por ids y por lugar; verificar con pruebas de servicio los filtros.

## 2. Reglas de negocio

- [x] 2.1 Implementar el alta de un lugar con sus sectores y el cálculo de capacidad; verificar sectores numerados y no numerados.
- [x] 2.2 Rechazar sectores incoherentes y nombres repetidos acumulando todos los errores; verificar el detalle de cada error.
- [x] 2.3 Implementar consulta, listado, edición de datos del lugar y alta de un sector en un lugar existente, con 404 y 409; verificar los tres casos.

## 3. API REST y contrato

- [x] 3.1 Crear DTOs validados y mapeos sin exponer entidades JPA; verificar coordenadas, campos obligatorios, lista de sectores no vacía y capacidad mayor a cero.
- [x] 3.2 Implementar `LugarController` y `SectorController`, el manejador de errores uniforme (400, 404 y 409) y la documentación OpenAPI; verificar códigos y formato de error.
- [x] 3.3 Verificar que `GET /api/lugares/{id}` y `GET /api/sectores/{id}` exponen los campos que consumen Evento y Entrada y que no incluyen precio.

## 4. Integración y documentación

- [x] 4.1 Ejecutar `msvc-ubicacion` contra PostgreSQL, crear un lugar y consultarlo; verificado manualmente (alta con sector numerado y no numerado, capacidades 200 y 5000, y persistencia tras reiniciar el servicio).
- [ ] 4.1b Consumir el lugar desde Evento (configurar sectores y publicar) para verificar el flujo extremo a extremo.
- [x] 4.2 Documentar la API, los contratos y las decisiones pendientes en este change (`proposal.md` y `design.md`) y en la documentación OpenAPI del servicio (Swagger UI).
- [ ] 4.3 Revisar el change con el grupo antes de archivarlo y resolver la baja de sectores y lugares.
