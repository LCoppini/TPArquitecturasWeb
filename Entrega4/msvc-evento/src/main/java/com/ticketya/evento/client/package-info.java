/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 * Clients previstos:
 *  - LugarClient / SectorClient (validar id_lugar y sector)
 *  - EntradaClient (disponibilidad, ventas y coordinacion de cancelacion)
 *
 * E2 (ADR-0001): UsuarioClient ya no se usa para decidir roles; el gateway
 * autoriza por path y el servicio solo compara idOrganizador con X-User-Id.
 * Queda pendiente envolver estos clients con Circuit Breaker + timeout + fallback.
 */
package com.ticketya.evento.client;
