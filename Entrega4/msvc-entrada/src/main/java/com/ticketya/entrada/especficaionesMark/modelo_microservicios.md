# Modelo de microservicios

## Entidades generales

### `usuario`

  Campo        Tipo      Clave
  ------------ --------- -------
  id_usuario   ---       PK
  nombre       VARCHAR   
  dni          INTEGER   
  apellido     INTEGER   
  ciudad       VARCHAR   

### `usuario_rol`

  Campo        Tipo   Clave
  ------------ ------ --------
  id_usuario   ---    PK, FK
  id_rol       ---    PK, FK

### `rol`

  Campo    Tipo      Clave
  -------- --------- -------
  id_rol   ---       PK
  tipo     VARCHAR   

**Relaciones:** - `usuario` se relaciona con `usuario_rol` mediante
`id_usuario`. - `rol` se relaciona con `usuario_rol` mediante
`id_rol`. - `usuario` y `rol` mantienen una relación N:M a través de
`usuario_rol`.

------------------------------------------------------------------------

# Microservicio de usuario

### `Usuario`

  Campo        Tipo
  ------------ --------
  id_usuario   Long
  dni          Long
  nombre       String
  apellido     String
  ciudad       String

### `usuario_rol`

  Campo        Tipo
  ------------ ------
  id_usuario   Long
  id_rol       Long

### `Rol`

  Campo    Tipo
  -------- --------
  id_rol   Long
  nombre   String

**Relaciones:** - `Usuario` → `usuario_rol` - `Rol` → `usuario_rol` -
`usuario_rol` funciona como tabla intermedia entre usuarios y roles.

------------------------------------------------------------------------

# Microservicio de ubicación

### `Lugar`

  Campo       Tipo
  ----------- --------
  id          Long
  nombre      String
  direccion   String
  ubicacion   String

### `Sector`

  Campo           Tipo
  --------------- ---------
  id_sector       Long
  id_lugar        Long
  sector_nombre   String
  capacidad_max   Integer
  precio          Double

### Entidad `Sector` mostrada en el diagrama

  Campo       Tipo      Clave
  ----------- --------- -------
  id_lugar    Long      PK
  nombre      VARCHAR   
  direccion   VARCHAR   
  ubicacion   VARCHAR   

**Relación:** - `Sector.id_lugar` referencia a `Lugar.id`.

> **Nota:** En la imagen el segundo bloque aparece también con el nombre
> `Sector`, aunque sus atributos corresponden a `Lugar`. Se mantiene la
> información tal como aparece en el diagrama.

------------------------------------------------------------------------

# Microservicio de evento

### `Evento`

  Campo         Tipo
  ------------- ------------------------
  id            Long
  id_lugar      Long
  nombre        String
  descripcion   String
  tipo          String
  fecha_hora    TimeStamp
  estado        String
  artistas      List`<Artista>`{=html}

### `evento`

  Campo         Tipo        Clave
  ------------- ----------- -------
  id_evento     Long        PK
  id_lugar      Long        FK
  nombre        VARCHAR     
  descripcion   VARCHAR     
  tipo          VARCHAR     
  fecha_hora    TimeStamp   
  estado        VARCHAR     

### `Artista`

  Campo    Tipo
  -------- --------
  id       Long
  nombre   String
  genero   String

### `artista`

  Campo        Tipo      Clave
  ------------ --------- -------
  id_artista   Long      PK
  nombre       VARCHAR   
  genero       VARCHAR   

### `evento_artista`

  Campo        Tipo   Clave
  ------------ ------ --------
  id_artista   ---    PK, FK
  id_evento    ---    PK, FK

**Relaciones:** - `evento.id_lugar` referencia al lugar
correspondiente. - `evento_artista` relaciona `evento` con `artista`. -
`evento` y `artista` mantienen una relación N:M mediante
`evento_artista`.

------------------------------------------------------------------------

# Microservicio de entrada

### `Entrada`

  Campo        Tipo
  ------------ --------
  id_entrada   Long
  id_usuario   Long
  id_evento    Long
  precio       int
  codigoQr     string

### `registro_pago`

  Campo         Tipo
  ------------- -----------
  id_entrada    Long
  id_pago       ---
  fecha_pago    TimeStamp
  descripcion   String
  monto_total   int

### `pago`

  Campo        Tipo
  ------------ --------
  id_pago      Long
  id_usuario   Long
  estado       String
  medio_pago   ---

**Relaciones:** - `Entrada` se relaciona con `registro_pago` mediante
`id_entrada`. - `pago` se relaciona con `registro_pago` mediante
`id_pago`. - `Entrada` contiene referencias a `id_usuario` e
`id_evento`.

------------------------------------------------------------------------

# Entidades persistentes de entrada y pago

### `entrada`

  Campo        Tipo   Clave
  ------------ ------ -------
  id_entrada   ---    PK
  id_usuario   ---    
  id_evento    ---    
  id_sector    ---    
  precio       ---    

### `registro_pago`

  Campo         Tipo        Clave
  ------------- ----------- --------
  id_entrada    ---         PK, FK
  id_pago       ---         PK, FK
  fecha_pago    TimeStamp   
  monto_final   Double      
  descripcion   String      

### `pago`

  Campo        Tipo     Clave
  ------------ -------- --------
  id_pago      ---      PK, FK
  id_usuario   ---      
  Estado       String   
  medio_pago   String   

**Relaciones:** - `entrada` → `registro_pago` mediante `id_entrada`. -
`pago` → `registro_pago` mediante `id_pago`. - `registro_pago` funciona
como vínculo entre una entrada y un pago.

------------------------------------------------------------------------

# Resumen de microservicios

  ---------------------------------------------------------------------------
  Microservicio               Responsabilidad         Entidades
                              principal               
  --------------------------- ----------------------- -----------------------
  `microservicio-usuario`     Gestión de usuarios y   `Usuario`, `Rol`,
                              roles                   `usuario_rol`

  `microservicio-ubicacion`   Gestión de lugares y    `Lugar`, `Sector`
                              sectores                

  `microservicio-evento`      Gestión de eventos y    `Evento`, `Artista`,
                              artistas                `evento_artista`

  `microservicio-entrada`     Gestión de entradas y   `Entrada`,
                              pagos                   `registro_pago`, `pago`
  ---------------------------------------------------------------------------

## Relaciones entre microservicios

``` text
microservicio-usuario
        │
        │ id_usuario
        ▼
microservicio-entrada
        │
        ├── id_evento ──────► microservicio-evento
        │
        └── id_sector ──────► microservicio-ubicacion

microservicio-evento
        │
        └── id_lugar ───────► microservicio-ubicacion
```

## Observaciones

-   Cada microservicio mantiene sus propias entidades y
    responsabilidades.
-   `id_usuario`, `id_evento`, `id_lugar` e `id_sector` funcionan como
    referencias entre servicios.
-   En el microservicio de evento, la relación entre eventos y artistas
    se resuelve mediante `evento_artista`.
-   En el microservicio de entrada, `registro_pago` vincula una entrada
    con un pago.
