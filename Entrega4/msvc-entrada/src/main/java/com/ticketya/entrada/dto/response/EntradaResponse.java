package com.ticketya.entrada.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class EntradaResponse {

    private Long id;
    private Long idEvento;
    private Long idUsuario;
    private Long idSector;
    private Integer fila;
    private Integer asiento;
    private BigDecimal precio;
    private String codigoQr;
    private String estado;
    private Instant expiraEn;
}
