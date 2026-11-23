package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.EntradaCompraRequest;
import com.ticketya.entrada.dto.response.SectorResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Regla de asignacion automatica: si el cliente elige un sector numerado sin indicar fila ni asiento,
 * se le asigna el primer asiento libre (por fila y luego por asiento). Los asientos que el cliente
 * indico explicitamente en la misma Compra tambien cuentan como tomados.
 */
final class AsignacionAsientos {

    private AsignacionAsientos() {
    }

    static CompraRequest resolver(
            CompraRequest request,
            Map<Long, SectorResponse> sectores,
            Function<Long, Set<String>> ocupadosDeSector
    ) {
        Map<Long, Set<String>> tomadosPorSector = new HashMap<>();
        List<EntradaCompraRequest> resueltas = new ArrayList<>();
        for (EntradaCompraRequest seleccion : request.entradas()) {
            SectorResponse sector = sectores.get(seleccion.idSector());
            boolean sinAsiento = seleccion.fila() == null && seleccion.asiento() == null;
            if (!"NUMERADO".equalsIgnoreCase(sector.tipo()) || !sinAsiento) {
                resueltas.add(seleccion);
                continue;
            }
            if (sector.filas() == null || sector.asientosPorFila() == null) {
                throw new DimensionesInvalidasException();
            }
            Set<String> tomados = tomadosPorSector.computeIfAbsent(seleccion.idSector(), idSector -> {
                Set<String> claves = new HashSet<>(ocupadosDeSector.apply(idSector));
                request.entradas().stream()
                        .filter(item -> idSector.equals(item.idSector()) && item.fila() != null)
                        .forEach(item -> claves.add(item.fila() + ":" + item.asiento()));
                return claves;
            });
            resueltas.add(primerAsientoLibre(seleccion.idSector(), sector, tomados));
        }
        return new CompraRequest(request.idEvento(), resueltas);
    }

    private static EntradaCompraRequest primerAsientoLibre(Long idSector, SectorResponse sector, Set<String> tomados) {
        for (int fila = 1; fila <= sector.filas(); fila++) {
            for (int asiento = 1; asiento <= sector.asientosPorFila(); asiento++) {
                if (tomados.add(fila + ":" + asiento)) {
                    return new EntradaCompraRequest(idSector, fila, asiento);
                }
            }
        }
        throw new SinLugaresException();
    }

    static final class SinLugaresException extends RuntimeException {
    }

    static final class DimensionesInvalidasException extends RuntimeException {
    }
}
