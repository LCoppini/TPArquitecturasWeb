/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 * Clients previstos:
 *  - UsuarioClient (validar que id_organizador existe y tiene rol ORGANIZADOR)
 *  - UbicacionClient (validar id_lugar / sector)
 * E2: envolver con Circuit Breaker + timeout + fallback.
 */
package com.ticketya.evento.infrastructure.client;
