package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.dto.request.EventoPublicadoRequest;
import com.ticketya.notificaciones.dto.response.EventoPublicadoResponse;

public interface AvisoEventoPublicadoService {

    /**
     * Avisa a los clientes de la ciudad del evento que tienen entre sus preferidos a alguno de sus artistas.
     * Un solo email por usuario y evento, aunque siga a varios artistas.
     */
    EventoPublicadoResponse avisar(EventoPublicadoRequest request);
}
