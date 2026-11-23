package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.EntradaCompraRequest;
import com.ticketya.entrada.dto.response.SectorResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsignacionAsientosTest {

    private static final SectorResponse NUMERADO = new SectorResponse(30L, 6, 2, 3, "NUMERADO");
    private static final SectorResponse GENERAL = new SectorResponse(31L, 100, null, null, "NO_NUMERADO");

    @Test
    void asignaLosPrimerosAsientosLibresEnOrdenDeFilaYAsiento() {
        CompraRequest request = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, null, null), new EntradaCompraRequest(30L, null, null)));

        CompraRequest resuelta = AsignacionAsientos.resolver(request, Map.of(30L, NUMERADO), id -> Set.of());

        assertEquals(List.of(new EntradaCompraRequest(30L, 1, 1), new EntradaCompraRequest(30L, 1, 2)),
                resuelta.entradas());
    }

    @Test
    void saltaLosAsientosOcupados() {
        CompraRequest request = new CompraRequest(20L, List.of(new EntradaCompraRequest(30L, null, null)));

        CompraRequest resuelta = AsignacionAsientos.resolver(request, Map.of(30L, NUMERADO),
                id -> Set.of("1:1", "1:2", "1:3"));

        assertEquals(new EntradaCompraRequest(30L, 2, 1), resuelta.entradas().getFirst());
    }

    @Test
    void respetaLosAsientosElegidosEnLaMismaCompra() {
        CompraRequest request = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(30L, 1, 1), new EntradaCompraRequest(30L, null, null)));

        CompraRequest resuelta = AsignacionAsientos.resolver(request, Map.of(30L, NUMERADO), id -> Set.of());

        assertEquals(new EntradaCompraRequest(30L, 1, 1), resuelta.entradas().get(0));
        assertEquals(new EntradaCompraRequest(30L, 1, 2), resuelta.entradas().get(1));
    }

    @Test
    void noTocaSectoresNoNumeradosNiSeleccionesConAsiento() {
        CompraRequest request = new CompraRequest(20L, List.of(
                new EntradaCompraRequest(31L, null, null), new EntradaCompraRequest(30L, 2, 3)));

        CompraRequest resuelta = AsignacionAsientos.resolver(request, Map.of(30L, NUMERADO, 31L, GENERAL),
                id -> Set.of());

        assertEquals(request.entradas(), resuelta.entradas());
    }

    @Test
    void sinAsientosLibresLanzaSinLugares() {
        CompraRequest request = new CompraRequest(20L, List.of(new EntradaCompraRequest(30L, null, null)));

        assertThrows(AsignacionAsientos.SinLugaresException.class, () -> AsignacionAsientos.resolver(
                request, Map.of(30L, NUMERADO), id -> Set.of("1:1", "1:2", "1:3", "2:1", "2:2", "2:3")));
    }

    @Test
    void sectorNumeradoSinDimensionesEsInvalido() {
        CompraRequest request = new CompraRequest(20L, List.of(new EntradaCompraRequest(30L, null, null)));
        SectorResponse sinDimensiones = new SectorResponse(30L, 6, null, null, "NUMERADO");

        assertThrows(AsignacionAsientos.DimensionesInvalidasException.class, () -> AsignacionAsientos.resolver(
                request, Map.of(30L, sinDimensiones), id -> Set.of()));
    }
}
