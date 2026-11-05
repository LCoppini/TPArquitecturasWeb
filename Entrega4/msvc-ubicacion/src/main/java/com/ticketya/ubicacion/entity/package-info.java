/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos.
 *
 * Modelo esperado:
 *  - Lugar (id, nombre, direccion, ubicacion lat/long) - aggregate root, contiene sectores
 *  - Sector (id_sector, id_lugar, nombre, capacidad_max, tipo NUMERADO | NO_NUMERADO)
 *  - Asiento (fila, numero) - solo para sectores numerados (A DEFINIR por el grupo: entidad o value object)
 *  - TODO: el precio NO deberia vivir en Sector: el enunciado dice que el organizador define el precio de cada sector PARA CADA EVENTO (ver msvc-evento)
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.ubicacion.entity;
