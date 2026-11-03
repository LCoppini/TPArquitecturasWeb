/**
 * DOMINIO de msvc-ubicacion: Lugares (estadios, teatros, salas) y sus sectores. Un sector puede ser numerado (filas/asientos) o no numerado (zona con capacidad maxima).
 *
 * Aggregates / entidades / value objects / enums. Sin logica de infraestructura.
 * Las reglas de negocio viven ACA (metodos de las entidades), no en los controllers.
 * Nota: por pragmatismo las entidades llevan anotaciones JPA (@Entity). Si el grupo quiere DDD estricto, separar en infrastructure/persistence.
 *
 * Modelo esperado:
 *  - Lugar (id, nombre, direccion, ubicacion lat/long) - aggregate root, contiene sectores
 *  - Sector (id_sector, id_lugar, nombre, capacidad_max, tipo NUMERADO | NO_NUMERADO)
 *  - Asiento (fila, numero) - solo para sectores numerados (A DEFINIR por el grupo: entidad o value object)
 *  - TODO: el precio NO deberia vivir en Sector: el enunciado dice que el organizador define el precio de cada sector PARA CADA EVENTO (ver msvc-evento)
 */
package com.ticketya.ubicacion.domain.model;
