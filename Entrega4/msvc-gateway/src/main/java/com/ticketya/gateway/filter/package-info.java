/**
 * Filtros transversales:
 *  - Generacion/propagacion del header de correlacion (X-Trace-Id) hacia los microservicios (trazabilidad, E2).
 *  - Log de cada request: endpoint, usuario, codigo de respuesta y tiempo de procesamiento (E2).
 */
package com.ticketya.gateway.filter;
