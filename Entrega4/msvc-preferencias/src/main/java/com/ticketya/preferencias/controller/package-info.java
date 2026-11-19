/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints previstos (a definir en el change de OpenSpec):
 *  - POST /api/preferencias/clientes/{idUsuario}/compras: registra una compra (tipo y artistas del evento). Interno: lo invoca msvc-entrada al emitir una entrada
 *  - GET /api/preferencias/clientes/{idUsuario}: preferencias del cliente (tipos de evento, artistas favoritos, ciudad)
 *  - GET /api/preferencias/artistas/{idArtista}/clientes?ciudad=: clientes con ese artista favorito. Lo consulta msvc-notificaciones
 *  - GET /api/recomendaciones/clientes/{idUsuario}: eventos sugeridos
 *
 * Rutas expuestas por el gateway: /api/preferencias/**, /api/recomendaciones/**
 * Los endpoints internos (registro de compras, clientes por artista) se restringen en el gateway al implementar JWT.
 */
package com.ticketya.preferencias.controller;
