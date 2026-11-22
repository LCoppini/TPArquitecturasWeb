# frontend-catalogo

## ADDED Requirements

### Requirement: Listado público de eventos

La aplicación SHALL mostrar el catálogo de eventos consultando `GET /api/eventos` sin necesidad de sesión, con una tarjeta por evento que incluya el arte, la fecha, la categoría, el nombre, el precio "Desde" y el estado de venta.

#### Scenario: Hay eventos

- **WHEN** el backend devuelve eventos
- **THEN** se muestra una tarjeta por evento con su fecha en formato "Sáb 14 nov · 21:00" y el menor precio de sus sectores como "$ 48.500"

#### Scenario: Evento agotado o cancelado

- **WHEN** el estado del evento es `AGOTADO` o `CANCELADO`
- **THEN** la tarjeta se muestra atenuada, con la insignia correspondiente y sin precio de compra

#### Scenario: El backend no entrega un dato del diseño

- **WHEN** la respuesta no incluye la sede del evento
- **THEN** la tarjeta omite esa línea y no muestra un valor inventado

### Requirement: Filtro por categoría y paginación

La aplicación SHALL permitir filtrar por categoría y paginar los resultados, reflejando ambos en la dirección de la página para poder compartirla o recargarla.

#### Scenario: Se elige una categoría

- **WHEN** la persona elige un chip de categoría
- **THEN** se consulta con el parámetro `tipo` correspondiente, se vuelve a la primera página y el chip queda seleccionado

#### Scenario: Se cambia de página

- **WHEN** la persona elige otra página en la paginación
- **THEN** se consulta con la página elegida y la dirección refleja la página

#### Scenario: Se recarga con filtros en la dirección

- **WHEN** se abre la dirección con categoría y página
- **THEN** la pantalla muestra esa categoría y esa página

### Requirement: Estados de la pantalla

La pantalla SHALL mostrar un estado de carga con skeletons, un estado sin resultados con acción para limpiar el filtro y un estado de error con acción para reintentar.

#### Scenario: Carga

- **WHEN** la consulta está en curso
- **THEN** se muestran skeletons con la misma forma de las tarjetas

#### Scenario: Sin resultados

- **WHEN** la consulta devuelve una lista vacía
- **THEN** se muestra el mensaje de que no hay eventos y un botón "Limpiar filtros"

#### Scenario: Error de carga

- **WHEN** la consulta falla
- **THEN** se muestra el estado de error con el botón "Reintentar", que repite la consulta
