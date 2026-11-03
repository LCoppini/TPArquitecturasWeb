/**
 * CASOS DE USO (una interfaz + implementacion por caso o por aggregate). Orquestan dominio, repositorios, clients y publishers.
 * Aca van las transacciones (@Transactional) y las llamadas a otros microservicios (via infrastructure/client).
 *  - Agregar 'ciudad' al Lugar: la busqueda de eventos (punto 9) filtra por ciudad
 */
package com.ticketya.ubicacion.application.service;
