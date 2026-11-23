package com.ticketya.entrada.dto.response;

import java.math.BigDecimal;

public record PagoCompraResponse(
        Long idPago,
        Long idCompra,
        Long idUsuario,
        String estado,
        String medioDePago,
        BigDecimal total,
        String referenciaPasarela
) {
}
