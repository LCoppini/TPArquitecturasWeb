/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 * Clients previstos:
 *  - (ninguno: este servicio no consulta a otros)
 *
 * E2: envolver con Circuit Breaker + timeout + fallback.
 */
package com.ticketya.ubicacion.client;
