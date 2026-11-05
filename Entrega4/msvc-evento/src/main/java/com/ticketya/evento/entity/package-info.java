/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos.
 *
 * Modelo esperado:
 *  - Evento (id, id_lugar, id_organizador, nombre, descripcion, tipo, fecha_hora, estado) - aggregate root
 *  - id_lugar e id_organizador son REFERENCIAS por id (sin FK): se validan consultando a msvc-ubicacion y msvc-usuarios
 *  - El organizador NO se guarda como entidad aca: solo su id; los datos se consultan al crear el evento via UsuarioClient
 *  - Artista (id, nombre, genero) - N:M con Evento (tabla evento_artista)
 *  - EventoSector (id_evento, id_sector, precio) - precio por sector para cada evento (propuesta, ver msvc-ubicacion)
 *  - EstadoEvento (enum) / TipoEvento
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.evento.entity;
