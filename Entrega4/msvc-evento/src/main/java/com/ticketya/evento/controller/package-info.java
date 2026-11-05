/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints previstos:
 *  - EventoController: CRUD, publicar, suspender/cancelar (ADMIN)
 *  - EventoController o BusquedaController: GET /api/eventos?tipo=&organizador=&participante=&ciudad=&desde=&hasta=&orden=&page=
 *  - ArtistaController
 *
 * Rutas expuestas por el gateway: /api/eventos/**, /api/artistas/**
 */
package com.ticketya.evento.controller;
