/**
 * E2 (EDA): consumo de eventos de otros servicios (listener). Los listeners deben ser IDEMPOTENTES.
 *
 * Eventos a consumir (ver docs/eventos.md):
 *  - EntradaEmitida (msvc-entrada): registrar tipo de evento y artistas de la compra para ese cliente
 *
 * Mecanismo (broker o llamada REST asincrona): a definir por el grupo y registrar en docs/decisiones.
 */
package com.ticketya.preferencias.messaging;
