/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints previstos:
 *  - UsuarioController: CRUD de usuarios y reemplazo de roles
 *  - RolController: listado de roles
 *
 * Rutas expuestas por el gateway: /api/usuarios/**, /api/roles/**
 */
package com.ticketya.usuarios.controller;
