/**
 * CAPA DE NEGOCIO. Interfaces de los servicios (ej. IEventoService). Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio, las transacciones (@Transactional) y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 * Responsabilidad de este servicio:
 *  - Lugares (estadios, teatros, salas) y sus sectores. Un sector puede ser numerado (filas/asientos) o no numerado (zona con capacidad maxima).
 *
 * Notas:
 *  - Agregar 'ciudad' al Lugar: la busqueda de eventos (punto 9) filtra por ciudad
 */
package com.ticketya.ubicacion.service;
