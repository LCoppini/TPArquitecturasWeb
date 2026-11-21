/**
 * CAPA DE PRESENTACION. Los controllers leen unicamente `X-User-Id` del gateway
 * (ADR-0001); no declaran ni propagan `X-User-Role` y no consultan roles por
 * Feign. La autorizacion por path ocurre en `msvc-gateway`.
 *
 * El contrato interno `/internal/entradas/disponibilidad` no tiene ruta publica
 * en el gateway: lo consume `msvc-evento` por Feign.
 */
package com.ticketya.entrada.controller;
