/**
 * EDA: eventos de dominio publicados por este servicio (clases inmutables, p. ej. records).
 * Nombrar en pasado: algo que YA ocurrio.
 *
 * Eventos previstos:
 *  - UsuarioRegistrado (E2: preferencias / notificaciones)
 *
 * Cada servicio define sus propios eventos (sin libreria compartida) para no acoplar los despliegues.
 * Catalogo completo en docs/eventos.md.
 */
package com.ticketya.usuarios.domain.event;
