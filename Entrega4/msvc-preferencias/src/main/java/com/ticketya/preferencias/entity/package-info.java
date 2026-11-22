/**
 * CAPA DE PERSISTENCIA. Documentos MongoDB (@Document) en la base propia ticketya_preferencias.
 * Se mantiene el nombre del paquete `entity` para respetar la estructura de los demas servicios.
 *
 * Modelo:
 *  - PreferenciaCliente (@Document "preferencias_cliente"): un documento por cliente
 *      _id = idUsuario (REFERENCIA por id, sin FK ni entidades de otros servicios)
 *      ciudad
 *      tiposEvento: tipo -> cantidad de compras
 *      artistas: lista de (idArtista, nombre, cantidad de compras)
 *      comprasProcesadas: ids de entrada ya registradas (idempotencia por idEntrada)
 *      eventosComprados: ids de eventos ya comprados (exclusion EP-17 en recomendaciones)
 *      actualizadoEn
 *
 * Por que MongoDB: documento anidado por cliente, esquema flexible y lecturas por id.
 * Justificacion en docs/decisiones/0004-mongodb-preferencias.md.
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.preferencias.entity;
