package com.ticketya.preferencias.dto.response;

import java.time.Instant;

public record RecomendacionResponse(
        Long idEvento,
        Instant fechaHora,
        String tipo,
        String motivo
) {
}
