package com.ticketya.evento.dto.response;

/** Datos del lugar que el cliente necesita para ver un evento. */
public record LugarDetalleResponse(String nombre, String direccion, String ciudad) {
}
