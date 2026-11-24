package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PagoCompraRequest(@NotBlank String medioDePago) {
}
