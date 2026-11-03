/**
 * TEST CLAVE DE LA ENTREGA 1: N hilos intentan comprar el MISMO asiento (o el ultimo lugar de una zona).
 * Verificar que exactamente UNA compra es exitosa y el resto falla (409) sin duplicar entradas.
 * Incluir caso de reintento con la misma clave de idempotencia (no genera una segunda compra).
 */
package com.ticketya.entrada.concurrencia;
