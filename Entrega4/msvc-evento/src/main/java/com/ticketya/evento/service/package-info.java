/**
 * CAPA DE NEGOCIO. Interfaces de los servicios (ej. IEventoService). Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio, las transacciones (@Transactional) y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 * Responsabilidad de este servicio:
 *  - Catalogo de eventos: ciclo de vida (borrador, publicado, agotado, cancelado, finalizado), artistas/participantes, precios por sector y busqueda multicriterio.
 *
 * Notas:
 *  - Busqueda: service con Specifications/Criteria (filtros) + orden (proximos, menor precio, mayor precio, mas populares)
 *  - E2: candidato natural a MongoDB / cache de catalogo (justificar en docs/decisiones)
 */
package com.ticketya.evento.service;
