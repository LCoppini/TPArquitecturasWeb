# Spec Delta

## Purpose

Selecciona eventos sugeridos para un cliente a partir de sus gustos registrados, indicando el motivo de cada sugerencia y aplicando un fallback por ciudad cuando no hay historial o no hay coincidencias.

## ADDED Requirements

### Requirement: Recomendación por coincidencia [Entrega 2]

Cuando existan eventos que coincidan con los gustos del cliente, el sistema DEBE devolverlos indicando si el motivo es el tipo de evento o un artista favorito.

#### Scenario: coincidencia por tipo

- **WHEN** un evento futuro tiene el mismo tipo que una preferencia del cliente
- **THEN** el sistema lo devuelve con motivo `MISMO_TIPO`

#### Scenario: coincidencia por artista

- **WHEN** un evento futuro incluye un artista favorito del cliente
- **THEN** el sistema lo devuelve con motivo `MISMO_ARTISTA`

### Requirement: Tope de artistas considerados [Entrega 2]

Cuando el cliente tenga más de cinco artistas registrados, el sistema DEBE considerar solo los cinco con mayor cantidad de compras, con desempate por identificador de artista ascendente.

#### Scenario: más de cinco artistas

- **WHEN** el cliente tiene doce artistas registrados
- **THEN** solo se usan los cinco de mayor cantidad para buscar coincidencias
- **AND** el orden es determinístico

### Requirement: Tope de tipos considerados [Entrega 2]

Cuando el cliente tenga más de tres tipos registrados, el sistema DEBE considerar solo los tres con mayor cantidad de compras, con desempate por tipo ascendente.

#### Scenario: más de tres tipos

- **WHEN** el cliente tiene cinco tipos registrados
- **THEN** solo se usan los tres de mayor cantidad para buscar coincidencias

### Requirement: Fallback sin coincidencias [Entrega 2]

Si el cliente tiene preferencias pero ningún evento coincide, el sistema DEBE sugerir eventos de la ciudad del cliente con motivo `FALLBACK_CIUDAD`, ordenados por fecha y desde el momento actual.

#### Scenario: sin coincidencias

- **WHEN** no hay eventos que coincidan con los gustos del cliente
- **THEN** el sistema consulta eventos de su ciudad desde el momento actual ordenados por fecha
- **AND** cada sugerencia lleva motivo `FALLBACK_CIUDAD`

### Requirement: Fallback sin documento de preferencia [Entrega 2]

Si el cliente nunca compró y no tiene documento de preferencia, el sistema DEBE obtener su ciudad del servicio de usuarios y usarla en el fallback con motivo `FALLBACK_CIUDAD`.

#### Scenario: cliente sin historial

- **WHEN** un cliente sin compras pide recomendaciones
- **THEN** el sistema obtiene su ciudad del servicio de usuarios
- **Y** sugiere eventos de esa ciudad desde el momento actual ordenados por fecha

### Requirement: Exclusión de eventos pasados [Entrega 2]

El sistema NO DEBE devolver eventos cuya fecha ya pasó, aun cuando la búsqueda de eventos con filtros los incluya.

#### Scenario: evento pasado coincidente

- **WHEN** un evento pasado coincide con un artista favorito del cliente
- **THEN** el sistema no lo incluye entre las recomendaciones

### Requirement: Consulta de recomendaciones con pertenencia [Entrega 2]

El sistema DEBE exponer `GET /api/recomendaciones/clientes/{idUsuario}` para `CLIENTE`, comparando el identificador del path contra `X-User-Id` y respondiendo 403 si difieren. DEBE exponer `GET /api/admin/recomendaciones/clientes/{idUsuario}` para `ADMINISTRADOR`, sin aplicar esa comparación. El gateway DEBE autorizar cada path por rol, sin propagar el rol al servicio.

#### Scenario: cliente pide recomendaciones ajenas

- **WHEN** un cliente llama `GET /api/recomendaciones/clientes/{idUsuario}` con un identificador distinto al de `X-User-Id`
- **THEN** el sistema responde 403

#### Scenario: cliente pide sus recomendaciones

- **WHEN** un cliente llama `GET /api/recomendaciones/clientes/{idUsuario}` con su propio identificador
- **THEN** el sistema devuelve la lista de eventos sugeridos con sus motivos

#### Scenario: administrador pide recomendaciones ajenas

- **WHEN** un administrador llama `GET /api/admin/recomendaciones/clientes/{idUsuario}` con cualquier identificador
- **THEN** el sistema devuelve las recomendaciones solicitadas

#### Scenario: rol usa el path ajeno

- **WHEN** un cliente llama un path `/api/admin/recomendaciones/**` o un administrador llama `GET /api/recomendaciones/clientes/{idUsuario}`
- **THEN** el gateway responde 403 antes de reenviar la solicitud
