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
    private Long idCompra;
    private String estadoDevolucion;
    private String motivoInvalidacion;

    /** Constructor previo, conservado para no modificar los tests existentes. */
    public EntradaResponse(Long id, Long idEvento, Long idUsuario, Long idSector, Integer fila, Integer asiento,
                           BigDecimal precio, String codigoQr, String estado, Instant expiraEn) {
        this(id, idEvento, idUsuario, idSector, fila, asiento, precio, codigoQr, estado, expiraEn,
                null, null, null);
    }
}
