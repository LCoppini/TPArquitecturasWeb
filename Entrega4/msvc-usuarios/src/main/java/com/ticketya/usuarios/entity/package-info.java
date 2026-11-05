/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos.
 *
 * Modelo esperado:
 *  - Usuario (id_usuario, nombre, apellido, dni, ciudad) - aggregate root
 *  - Rol (id_rol, nombre) - relacion N:M con Usuario (tabla usuario_rol)
 *  - Los roles se crean al inicializar la BD (seed): CLIENTE, ORGANIZADOR, ADMINISTRADOR, PERSONAL_ACCESO
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.usuarios.entity;
