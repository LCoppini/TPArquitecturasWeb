package com.ticketya.entrada.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RegistroPagoRequest {

    @NotNull
    private Long idEntrada;

    @NotNull
    private Long idPago;

    @NotNull
    private LocalDateTime fechaPago;

    @NotBlank
    private String descripcion;

    @NotNull
    private BigDecimal montoTotal;
}
