/**
 * Recepción de compras aprobadas: REST interno + outbox persistido en msvc-entrada.
 * No se usa broker. El worker de msvc-entrada envía
 * POST /internal/preferencias/compras y el registro es idempotente por idEntrada.
 *
 * El aviso fire-and-forget hacia msvc-notificaciones (clientes que tienen a un
 * artista como favorito) queda fuera de alcance de este change.
 */
package com.ticketya.preferencias.messaging;
