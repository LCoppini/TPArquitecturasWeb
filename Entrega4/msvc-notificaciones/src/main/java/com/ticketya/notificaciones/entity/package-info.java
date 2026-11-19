/**
 * CAPA DE PERSISTENCIA. Documentos MongoDB (@Document) y enums. Base propia: MongoDB ticketya_notificaciones.
 * Se mantiene el nombre del paquete `entity` para respetar la estructura de los demas servicios.
 *
 * Modelo esperado (base: sugerencia.md del grupo; ajustar en el change):
 *  - Notificacion (@Document "notificaciones"): un documento por aviso
 *      id (String/ObjectId)
 *      idUsuario: destinatario (REFERENCIA por id, sin FK)
 *      tipo: COMPRA_CONFIRMADA | DEVOLUCION_APROBADA | EVENTO_CANCELADO | EVENTO_MODIFICADO | ARTISTA_FAVORITO_PUBLICA_EVENTO
 *      idEvento (opcional): evento relacionado
 *      idEventoOrigen: id del evento de dominio que genero el aviso (evita duplicados ante reintentos)
 *      contenido: mapa con los datos que completan la plantilla del email
 *      canal: EMAIL (push simulado, si se agrega)
 *      estado: PENDIENTE | ENVIADA | FALLIDA
 *      intentos, creadaEn, enviadaEn
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.notificaciones.entity;
