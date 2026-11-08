# Modelo de datos y entidades

## Microservicio de usuario

### Entidades

#### `Usuario`

- `id_usuario`: Long
- `dni`: Long
- `nombre`: String
- `apellido`: String
- `ciudad`: String

#### `Rol`

- `id_rol`: Long
- `tipo`: String

#### `usuario_rol`

Tabla intermedia para relacionar usuarios con roles.

- `id_usuario`: Long
- `id_rol`: Long

### Esquema de base de datos

#### Tabla `usuario`

| Campo | Tipo | Clave |
|---|---|---|
| `id_usuario` |  | PK |
| `nombre` | VARCHAR | |
| `dni` | INTEGER | UNIQUE (aplica también a usuarios dados de baja) |
| `apellido` | VARCHAR | |
| `ciudad` | VARCHAR | |
| `activo` | BOOLEAN | NOT NULL, valor inicial `true` |

La referencia de usuario entre microservicios utiliza `id_usuario`; el DNI no se utiliza como identificador de API ni como clave foránea.

#### Tabla `rol`

| Campo | Tipo | Clave |
|---|---|---|
| `id_rol` | — | PK |
| `tipo` | VARCHAR | UNIQUE |

#### Tabla `usuario_rol`

| Campo | Tipo | Clave |
|---|---|---|
| `id_usuario` | — | PK, FK1 |
| `id_rol` | — | PK, FK2 |

---

## Microservicio de evento

### Entidades

#### `Evento`

- `id`: Long
- `id_lugar`: Long
- `nombre`: String
- `descripcion`: String
- `tipo`: String
- `fecha_hora`: TimeStamp
- `estado`: String
- `artistas`: List<Artista>

Métodos:
- `getters()`
- `setters()`

#### `Artista`

- `id`: Long
- `nombre`: String
- `genero`: String

Métodos:
- `getters()`
- `setters()`

### Esquema de base de datos

#### Tabla `evento`

| Campo | Tipo | Clave |
|---|---|---|
| `id_evento` | — | PK |
| `id_lugar` | — | FK |
| `fecha_hora` | TimeStamp | |
| `nombre` | VARCHAR | |
| `descripcion` | VARCHAR | |
| `tipo` | VARCHAR | |
| `estado` | VARCHAR | |

#### Tabla `artista`

| Campo | Tipo | Clave |
|---|---|---|
| `id_artista` | — | PK |
| `nombre` | VARCHAR | |
| `genero` | VARCHAR | |

#### Tabla `evento_artista`

Tabla intermedia entre eventos y artistas.

| Campo | Tipo | Clave |
|---|---|---|
| `id_artista` | — | PK, FK |
| `id_evento` | — | PK, FK |

---

## Microservicio de ubicación

### Entidades

#### `Lugar`

- `id`: Long
- `nombre`: String
- `direccion`: String
- `ubicacion`: String

Métodos:
- `getters()`
- `setters()`

#### `Sector`

- `id_sector`: Long
- `id_lugar`: Long
- `sector_nombre`: String
- `capacidad_max`: Integer
- `precio`: Double

### Esquema de base de datos

#### Tabla `Sector`

| Campo | Tipo | Clave |
|---|---|---|
| `id_lugar` | — | PK |
| `nombre` | VARCHAR | |
| `direccion` | VARCHAR | |
| `ubicacion` | VARCHAR | |

#### Tabla `Sector` (detalle de sectores)

| Campo | Tipo | Clave |
|---|---|---|
| `id_sector` | — | PK |
| `id_lugar` | — | FK |
| `sector_nombre` | VARCHAR | |
| `capacidad_max` | INT | |
| `precio` | Double | |

---

## Microservicio de entrada

### Entidades

#### `Entrada`

- `id_entrada`: Long
- `id_usuario`: Long
- `id_evento`: Long
- `precio`: int
- `codigoQr`: string
- `id_sector`: Long
- `claveIdempotencia`: String
- `estadoDeDevolucion`

#### `pago`

- `id_pago`: Long
- `id_usuario`: Long
- `estado`: String
- `medio_pago`: String
- `id_entrada`: Long
- `claveIdempotencia`: String
- `referenciaPasarela`: String
- `huellaPasarela`: String

#### `registro_pago`

- `id_entrada`: Long
- `id_pago`
- `fecha_pago`: TiemStam
- `descripcion`: String
- `monto_total`: int

### Esquema de base de datos

#### Tabla `entrada`

| Campo | Tipo / observación |
|---|---|
| `id_entrada` | PK |
| `id_usuario` | |
| `id_evento` | |
| `id_sector` | |
| `precio` | |
| `codigoQr` | |
| `claveIdempotencia` | |
| `estadoDeDevolucion` | |

#### Tabla `pago`

| Campo | Tipo / observación |
|---|---|
| `id_pago` | PK, FK1 |
| `id_usuario` | |
| `estado` | String |
| `medio_pago` | String |

#### Tabla `registro_pago`

| Campo | Tipo / observación |
|---|---|
| `id_entrada` | PK, FK1 |
| `id_pago` | PK, FK2 |
| `fecha_pago` | TimeStamp |
| `monto_final` | Double |
| `descripcion` | String |

---
