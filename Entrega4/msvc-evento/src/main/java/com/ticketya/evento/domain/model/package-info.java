/**
 * DOMINIO de msvc-evento: Catalogo de eventos: ciclo de vida (borrador, publicado, agotado, cancelado, finalizado), artistas/participantes, precios por sector y busqueda multicriterio.
 *
 * Aggregates / entidades / value objects / enums. Sin logica de infraestructura.
 * Las reglas de negocio viven ACA (metodos de las entidades), no en los controllers.
 * Nota: por pragmatismo las entidades llevan anotaciones JPA (@Entity). Si el grupo quiere DDD estricto, separar en infrastructure/persistence.
 *
 * Modelo esperado:
 *  - Evento (id, id_lugar, id_organizador, nombre, descripcion, tipo, fecha_hora, estado) - aggregate root
 *  - id_lugar e id_organizador son REFERENCIAS por id (sin FK): se validan consultando a msvc-ubicacion y msvc-usuarios
 *  - El organizador NO se guarda como entidad aca: solo su id; los datos se consultan al crear el evento via UsuarioClient
 *  - Artista (id, nombre, genero) - N:M con Evento (tabla evento_artista)
 *  - EventoSector (id_evento, id_sector, precio) - precio por sector para cada evento (propuesta, ver msvc-ubicacion)
 *  - EstadoEvento (enum) / TipoEvento
 */
package com.ticketya.evento.domain.model;
