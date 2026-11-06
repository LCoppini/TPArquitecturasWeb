package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PagoRequest {

    @NotNull
    private Long idUsuario;

    @NotBlank
    private String medioDePago;

    @NotNull
    private Long idEntrada;

    @NotBlank
    private String claveIdempotencia;
}
