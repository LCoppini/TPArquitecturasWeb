/**
 * Clients Feign hacia OTROS microservicios (resueltos por Eureka: nombre msvc-xxx).
 *  - UsuarioClient (msvc-usuarios): email del destinatario cuando la solicitud no lo trae. OJO: hoy UsuarioResponse de
 *    msvc-usuarios no expone email; mientras tanto el llamador debe enviar "destinatario".
 *
 * Pendiente (fuera de este change): PreferenciasClient (msvc-preferencias) para "artista favorito publica un evento".
 * Timeouts Feign en msvc-notificaciones.yml (clave usuarioClient = contextId).
 */
package com.ticketya.notificaciones.client;
