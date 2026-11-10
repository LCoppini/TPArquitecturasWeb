package com.ticketya.evento.dto.response;

import java.util.List;

public record EventoPaginaResponse(
        List<EventoResponse> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
