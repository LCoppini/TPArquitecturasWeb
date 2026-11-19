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
 *  - Version simplificada de recomendaciones (sin co-compra "quienes compraron esto tambien compraron"): justificar en docs/decisiones.
 *  - El registro de una compra debe ser idempotente.
 */
package com.ticketya.preferencias.service;
