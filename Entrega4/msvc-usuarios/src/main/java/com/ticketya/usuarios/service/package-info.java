/**
 * CAPA DE NEGOCIO. Interfaces de los servicios (ej. IEventoService). Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio, las transacciones (@Transactional) y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 * Responsabilidad de este servicio:
 *  - Usuarios y roles (CLIENTE, ORGANIZADOR, ADMINISTRADOR, PERSONAL_ACCESO). Fuente de verdad de la identidad: el resto de los servicios solo guarda id_usuario.
 *
 * Notas:
 *  - E2: autenticacion (login -> JWT) vive en msvc-gateway; aca solo se expone la consulta de credenciales/roles
 */
package com.ticketya.usuarios.service;
