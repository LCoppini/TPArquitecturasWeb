/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 * Clients previstos:
 *  - EventoClient (msvc-evento): tipo y artistas de un evento; eventos candidatos para recomendar
 *  - UsuarioClient (msvc-usuarios): ciudad del cliente (opcional)
 *
 * E2: envolver con Circuit Breaker + timeout + fallback.
 */
package com.ticketya.preferencias.client;
