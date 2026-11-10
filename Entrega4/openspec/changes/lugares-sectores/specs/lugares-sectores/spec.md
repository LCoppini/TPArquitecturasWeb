# Spec Delta

## Purpose

Definir cómo `msvc-ubicacion` modela los lugares y la estructura física de sus sectores, y qué contratos expone a Evento y Entrada.

## ADDED Requirements

### Requirement: Alta de lugares con sectores (Entrega 1)
Cuando se crea un lugar, el sistema deberá (SHALL) guardar su nombre, dirección, ciudad, latitud y longitud junto con al menos un sector, y devolver el lugar creado con los ids asignados.

#### Scenario: Lugar creado con sectores
- **WHEN** se envía un lugar con nombre, dirección, ciudad, coordenadas válidas y al menos un sector válido
- **THEN** responde 201 con el lugar y cada sector con su id

#### Scenario: Lugar sin sectores
- **WHEN** se envía un lugar con la lista de sectores vacía
- **THEN** responde 400 indicando que `sectores` es obligatorio y no crea el lugar

#### Scenario: Coordenadas inválidas
- **WHEN** se envía una latitud fuera de -90 a 90 o una longitud fuera de -180 a 180
- **THEN** responde 400 indicando cada campo inválido y no crea el lugar

### Requirement: Capacidad de los sectores (Entrega 1)
El sistema deberá (SHALL) calcular la capacidad de un sector numerado como filas por asientos por fila y deberá (SHALL) exigir una capacidad máxima mayor a cero en un sector no numerado.

#### Scenario: Sector numerado
- **WHEN** se crea un sector `NUMERADO` con 10 filas y 20 asientos por fila
- **THEN** el sector queda con capacidad 200

#### Scenario: Sector numerado con capacidad incoherente
- **WHEN** se crea un sector `NUMERADO` con filas y asientos por fila y una capacidad distinta de su producto
- **THEN** responde 400 indicando la capacidad calculada

#### Scenario: Sector no numerado
- **WHEN** se crea un sector `NO_NUMERADO` con capacidad máxima 5000
- **THEN** el sector queda con capacidad 5000 y sin filas ni asientos por fila

#### Scenario: Sector no numerado sin capacidad válida
- **WHEN** se crea un sector `NO_NUMERADO` sin capacidad máxima o con capacidad menor o igual a cero
- **THEN** responde 400 indicando que se requiere una capacidad mayor a cero

### Requirement: Validación acumulada de sectores (Entrega 1)
Cuando una solicitud contiene varios errores, el sistema deberá (SHALL) responder 400 con el detalle de cada error identificando el sector afectado.

#### Scenario: Varios sectores inválidos
- **WHEN** se envía un lugar con un sector numerado sin filas y otro no numerado sin capacidad
- **THEN** responde 400 con un mensaje que menciona `sectores[0]` y `sectores[1]`

#### Scenario: Nombres repetidos
- **WHEN** se envían dos sectores del mismo lugar con el mismo nombre, sin distinguir mayúsculas
- **THEN** responde 400 indicando el nombre repetido

### Requirement: Consulta de lugares (Entrega 1)
El sistema deberá (SHALL) permitir consultar un lugar con sus sectores por id, y listar lugares filtrados por ciudad sin distinguir mayúsculas o por una lista de ids.

#### Scenario: Consulta por id
- **WHEN** se consulta `GET /api/lugares/{id}` de un lugar existente
- **THEN** responde 200 con `id`, `nombre`, `direccion`, `ciudad`, `ubicacion`, `latitud`, `longitud` y sus sectores

#### Scenario: Lugar inexistente
- **WHEN** se consulta un lugar que no existe
- **THEN** responde 404 con el formato de error común

#### Scenario: Filtro por ciudad
- **WHEN** se consulta `GET /api/lugares?ciudad=tandil`
- **THEN** responde 200 con los lugares cuya ciudad es "Tandil", sin distinguir mayúsculas

### Requirement: Consulta de sectores sin precio (Entrega 1)
El sistema deberá (SHALL) permitir consultar un sector por id con su lugar, tipo y capacidad, y no deberá (SHALL NOT) incluir precio ni disponibilidad.

#### Scenario: Consulta de sector por id
- **WHEN** se consulta `GET /api/sectores/{id}` de un sector existente
- **THEN** responde 200 con `idSector`, `idLugar`, `nombre`, `tipo`, `filas`, `asientosPorFila` y `capacidadMax`, sin precio

#### Scenario: Sector inexistente
- **WHEN** se consulta un sector que no existe
- **THEN** responde 404 con el formato de error común

### Requirement: Modificación de lugares y alta de sectores (Entrega 1)
El sistema deberá (SHALL) permitir editar los datos de un lugar y agregar sectores a un lugar existente, y no deberá (SHALL NOT) modificar ni eliminar sectores ya creados, para mantener estables los ids que otros servicios guardan.

#### Scenario: Edición de datos del lugar
- **WHEN** se envía una edición válida de un lugar existente
- **THEN** responde 200, se actualizan nombre, dirección, ciudad y coordenadas, y sus sectores conservan sus ids

#### Scenario: Sector agregado
- **WHEN** se agrega un sector válido a un lugar existente
- **THEN** responde 201 con el sector asociado a ese lugar

#### Scenario: Nombre de sector ya usado
- **WHEN** se agrega un sector cuyo nombre ya existe en el lugar, sin distinguir mayúsculas
- **THEN** responde 409 y no crea el sector
