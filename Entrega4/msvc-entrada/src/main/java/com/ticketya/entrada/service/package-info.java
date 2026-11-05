/**
 * CAPA DE NEGOCIO. Interfaces de los servicios (ej. IEventoService). Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio, las transacciones (@Transactional) y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 * Responsabilidad de este servicio:
 *  - Compra de entradas: reserva temporal, pago (mock), emision con QR, devoluciones y validacion de acceso. Aca vive el mecanismo anti doble venta.
 *
 * Notas:
 *  - Saga de compra (reservar -> pagar -> emitir, con compensacion): service/saga (E2; en E1 orquestacion simple)
 *  - Pasarela de pagos simulada: client/pasarela (interfaz + implementacion mock)
 *  - Job de vencimiento de reservas: scheduler
 */
package com.ticketya.entrada.service;
