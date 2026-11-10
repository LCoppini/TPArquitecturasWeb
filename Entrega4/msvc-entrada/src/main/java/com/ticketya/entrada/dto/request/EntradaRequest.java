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

    // Se conserva para compatibilidad con clientes existentes, pero el precio se obtiene de msvc-evento.
    private BigDecimal precio;

    private String codigoQr;

    // Selección validada con la configuración del sector obtenida desde msvc-ubicacion.
    private Integer fila;

    private Integer asiento;

    private String claveIdempotencia;

    private Boolean aprobarDevolucion;
}
