/**
 * E2 (EDA): consumo de eventos de otros servicios (listener). Los listeners deben ser IDEMPOTENTES.
 *
 * Eventos a consumir (ver docs/eventos.md):
 *  - EntradaEmitida, DevolucionAprobada (msvc-entrada)
 *  - EventoCancelado, EventoModificado, EventoPublicado (msvc-evento)
 *
 * Mecanismo (broker o llamada REST asincrona fire-and-forget): a definir por el grupo y registrar en docs/decisiones.
 */
package com.ticketya.notificaciones.messaging;
