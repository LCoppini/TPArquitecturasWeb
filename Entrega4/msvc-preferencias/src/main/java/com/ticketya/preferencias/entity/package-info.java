/**
 * CAPA DE PERSISTENCIA. Documentos MongoDB (@Document) y enums. Base propia: MongoDB ticketya_preferencias.
 * Se mantiene el nombre del paquete `entity` para respetar la estructura de los demas servicios.
 *
 * Modelo esperado (propuesta, ajustar en el change):
 *  - PreferenciaCliente (@Document "preferencias"): un documento por cliente
 *      id = id_usuario (REFERENCIA por id, sin FK)
 *      ciudad
 *      tiposEvento: tipo -> cantidad de compras
 *      artistas: lista de (idArtista, nombre, cantidad de compras)
 *      comprasProcesadas: ids de entrada ya registradas (idempotencia: una entrada no suma dos veces)
 *      actualizadoEn
 *
 * Por que MongoDB: documento anidado por cliente, esquema flexible y lecturas por id. Justificar en docs/decisiones.
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.preferencias.entity;
