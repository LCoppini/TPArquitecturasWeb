/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 * Clients previstos:
 *  - UsuarioClient (validar cliente)
 *  - EventoClient (estado del evento, precio del sector)
 *  - UbicacionClient (capacidad / asientos del sector)
 * E2: envolver con Circuit Breaker + timeout + fallback.
 */
package com.ticketya.entrada.infrastructure.client;
