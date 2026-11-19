/**
 * CAPA DE NEGOCIO. Interfaces de los servicios. Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 * Responsabilidad de este servicio:
 *  - Registrar automaticamente las preferencias del cliente a partir de su historial de compras (sin marcado manual).
 *  - Recomendar eventos del mismo tipo y/o con los mismos artistas favoritos del cliente.
 *  - Informar que clientes tienen a un artista como favorito (para las notificaciones).
 *
 * Notas:
 *  - Criterio de recomendacion a definir: sugerencia.md propone afinidad, co-ocurrencia o fallback popular;
 *    el config.yaml contempla solo afinidad (tipo y artistas). Justificar la version simplificada en docs/decisiones.
 *  - El registro de una compra debe ser idempotente (deduplicar por idMensaje).
 */
package com.ticketya.preferencias.service;
