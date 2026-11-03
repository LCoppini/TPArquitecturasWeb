/**
 * CASOS DE USO (una interfaz + implementacion por caso o por aggregate). Orquestan dominio, repositorios, clients y publishers.
 * Aca van las transacciones (@Transactional) y las llamadas a otros microservicios (via infrastructure/client).
 *  - E2: autenticacion (login -> JWT) vive en msvc-gateway; aca solo se expone la consulta de credenciales/roles
 */
package com.ticketya.usuarios.application.service;
