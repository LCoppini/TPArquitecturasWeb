/**
 * CASOS DE USO (una interfaz + implementacion por caso o por aggregate). Orquestan dominio, repositorios, clients y publishers.
 * Aca van las transacciones (@Transactional) y las llamadas a otros microservicios (via infrastructure/client).
 *  - Saga de compra (reservar -> pagar -> emitir, con compensacion): application/service/saga (E2; en E1 orquestacion simple)
 *  - Pasarela de pagos simulada: infrastructure/client/pasarela (interfaz + implementacion mock)
 *  - Job de vencimiento de reservas: infrastructure/scheduler
 */
package com.ticketya.entrada.application.service;
