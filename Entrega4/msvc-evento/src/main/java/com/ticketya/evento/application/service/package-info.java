/**
 * CASOS DE USO (una interfaz + implementacion por caso o por aggregate). Orquestan dominio, repositorios, clients y publishers.
 * Aca van las transacciones (@Transactional) y las llamadas a otros microservicios (via infrastructure/client).
 *  - Busqueda: domain/service o application/service con Specifications/Criteria (filtros) + orden (proximos, menor precio, mayor precio, mas populares)
 *  - E2: candidato natural a MongoDB / cache de catalogo (justificar en docs/decisiones)
 */
package com.ticketya.evento.application.service;
