/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 * Clients previstos:
 *  - PreferenciasClient (msvc-preferencias): clientes cuyo artista/tipo/ciudad favorito coincide con un evento recien publicado
 *  - UsuarioClient (msvc-usuarios): datos del destinatario. OJO: hoy Usuario NO tiene email; definir con el grupo (ver README del servicio)
 *
 * E2: envolver con Circuit Breaker + timeout + fallback.
 */
package com.ticketya.notificaciones.client;
