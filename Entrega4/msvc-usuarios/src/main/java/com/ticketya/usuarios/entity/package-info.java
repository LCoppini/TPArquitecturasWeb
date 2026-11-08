/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos.
 *
 * Modelo:
 *  - Usuario (id_usuario, dni unico, nombre, apellido, ciudad, activo) - aggregate root
 *  - Rol (id_rol, tipo unico) - relacion N:M con Usuario (tabla usuario_rol)
 *  - Los roles se crean al inicializar la BD (seed): CLIENTE, ORGANIZADOR, ADMINISTRADOR, PERSONAL_ACCESO
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.usuarios.entity;
