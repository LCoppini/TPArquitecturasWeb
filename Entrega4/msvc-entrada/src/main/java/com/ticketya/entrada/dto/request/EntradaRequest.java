package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class EntradaRequest {

    @NotNull
    private Long idEvento;

    private Long idUsuario;

    @NotNull
    private Long idSector;

    @NotNull
    private BigDecimal precio;

    private String codigoQr;

    // Selección validada con la configuración del sector obtenida desde msvc-ubicacion.
    private Integer fila;

    private Integer asiento;

    private String claveIdempotencia;

    private Boolean aprobarDevolucion;
}
