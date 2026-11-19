/**
 * CAPA DE PERSISTENCIA. Documentos MongoDB (@Document) y enums. Base propia: MongoDB ticketya_preferencias.
 * Se mantiene el nombre del paquete `entity` para respetar la estructura de los demas servicios.
 *
 * Modelo esperado (base: sugerencia.md del grupo; ajustar en el change):
 *  - Preferencia (@Document "preferencias"): un documento por cliente
 *      id = idUsuario (REFERENCIA por id, sin FK)
 *      tiposEvento: lista de textos
 *      artistasFavoritos: ids de artistas del catalogo de msvc-evento
 *      ciudad
 *      historialCompras: lista de compras (idMensaje unico para deduplicar, idEvento, fecha, tipo y artistas)
 *      actualizadaEn
 *  - Recomendacion (@Document "recomendaciones"): un lote generado para un cliente
 *      id, idUsuario, generadaEn, ciudad, criterio
 *      eventos: lista de (idEvento, nombre y tipo como snapshot, fecha, puntuacion, motivo)
 *      expiraEn (opcional)
 *
 * Por que MongoDB: documento anidado por cliente, esquema flexible y lecturas por id. Justificar en docs/decisiones.
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.preferencias.entity;
