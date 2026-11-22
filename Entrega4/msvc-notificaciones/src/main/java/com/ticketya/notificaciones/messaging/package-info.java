/**
 * E2 (EDA): sin broker en este servicio. Los otros microservicios lo invocan por REST interno
 * (POST /internal/notificaciones, fire-and-forget) y el envio es asincrono DENTRO del servicio (@Async + pool propio),
 * decision del grupo registrada en openspec/config.yaml. Si en el futuro se agrega un broker, los listeners
 * (EntradaEmitida, DevolucionAprobada, EventoCancelado, EventoModificado) van aca y deben ser IDEMPOTENTES.
 */
package com.ticketya.notificaciones.messaging;
