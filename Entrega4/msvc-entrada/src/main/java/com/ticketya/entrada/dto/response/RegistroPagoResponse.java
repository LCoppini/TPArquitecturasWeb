package com.ticketya.entrada.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RegistroPagoResponse {

    private Long idEntrada;
    private Long idPago;
    private LocalDateTime fechaPago;
    private String descripcion;
    private BigDecimal montoTotal;
}
