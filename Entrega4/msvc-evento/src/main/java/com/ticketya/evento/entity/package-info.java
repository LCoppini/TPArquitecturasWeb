/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos.
 *
 * Modelo esperado:
 *  - Evento (id, id_lugar, id_organizador, nombre, descripcion, tipo, fecha_hora, estado) - aggregate root
 *  - id_lugar se valida consultando msvc-ubicacion; id_organizador se compara con el X-User-Id del token (ADR-0001)
 *  - El organizador NO se guarda como entidad aca: solo su id
 *  - Artista (id, nombre, tipo) - N:M con Evento (tabla evento_artista)
 *  - EventoSector (id_evento, id_sector, precio) - precio por sector para cada evento
 *  - EstadosEvento (enum) / TipoEvento
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.evento.entity;
