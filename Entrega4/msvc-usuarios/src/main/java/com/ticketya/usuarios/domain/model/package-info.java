/**
 * DOMINIO de msvc-usuarios: Usuarios y roles (CLIENTE, ORGANIZADOR, ADMINISTRADOR, PERSONAL_ACCESO). Fuente de verdad de la identidad: el resto de los servicios solo guarda id_usuario.
 *
 * Aggregates / entidades / value objects / enums. Sin logica de infraestructura.
 * Las reglas de negocio viven ACA (metodos de las entidades), no en los controllers.
 * Nota: por pragmatismo las entidades llevan anotaciones JPA (@Entity). Si el grupo quiere DDD estricto, separar en infrastructure/persistence.
 *
 * Modelo esperado:
 *  - Usuario (id_usuario, nombre, apellido, dni, ciudad) - aggregate root
 *  - Rol (id_rol, nombre) - relacion N:M con Usuario (tabla usuario_rol)
 *  - Los roles se crean al inicializar la BD (seed): CLIENTE, ORGANIZADOR, ADMINISTRADOR, PERSONAL_ACCESO
 */
package com.ticketya.usuarios.domain.model;
