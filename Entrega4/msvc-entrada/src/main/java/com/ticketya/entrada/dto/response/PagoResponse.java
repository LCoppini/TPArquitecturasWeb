package com.ticketya.entrada.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PagoResponse {

    private Long idPago;
    private Long idUsuario;
    private String estado;
    private String medioDePago;
    private Long idEntrada;
    private String referenciaPasarela;
}
