package com.ticketya.evento.service;

import com.ticketya.evento.client.EntradaClient;
import com.ticketya.evento.client.SectorClient;
import com.ticketya.evento.dto.response.ReporteEventoResponse;
import com.ticketya.evento.dto.response.ReporteSectorEventoResponse;
import com.ticketya.evento.dto.response.ReporteSectorResponse;
import com.ticketya.evento.dto.response.ReporteVentasEventoResponse;
import com.ticketya.evento.dto.response.SectorResponse;
import com.ticketya.evento.dto.response.TotalesEventoResponse;
import com.ticketya.evento.entity.Evento;
import com.ticketya.evento.exception.AccesoDenegadoException;
import com.ticketya.evento.exception.RecursoNoEncontradoException;
import com.ticketya.evento.exception.ServicioExternoException;
import com.ticketya.evento.repository.EventoRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reporte público del organizador dueño del evento (ADR-0001 y ADR-0003). */
@Service
@Transactional(readOnly = true)
public class ReporteOrganizadorService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CERO = new BigDecimal("0.00");

    private final EventoRepository eventoRepository;
    private final SectorClient sectorClient;
    private final EntradaClient entradaClient;

    public ReporteOrganizadorService(EventoRepository eventoRepository,
                                     SectorClient sectorClient,
                                     EntradaClient entradaClient) {
        this.eventoRepository = eventoRepository;
        this.sectorClient = sectorClient;
        this.entradaClient = entradaClient;
    }

    public ReporteEventoResponse reporte(Long idEvento, String idUsuarioHeader) {
        Long idSolicitante = parsearIdUsuario(idUsuarioHeader);
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el evento con id " + idEvento));
        if (!evento.getIdOrganizador().equals(idSolicitante)) {
            throw new AccesoDenegadoException("Solo el organizador propietario puede consultar el reporte");
        }

        ReporteVentasEventoResponse ventas = consultarVentas(idEvento);
        List<ReporteSectorEventoResponse> porSector = new ArrayList<>();
        for (ReporteSectorResponse cifras : cifrasPorSector(evento, ventas).values()) {
            porSector.add(enriquecer(cifras));
        }

        long emitidas = porSector.stream().mapToLong(ReporteSectorEventoResponse::entradasEmitidas).sum();
        long activas = porSector.stream().mapToLong(ReporteSectorEventoResponse::entradasActivas).sum();
        long devueltas = porSector.stream().mapToLong(ReporteSectorEventoResponse::entradasDevueltas).sum();
        long enReintegro = porSector.stream().mapToLong(ReporteSectorEventoResponse::enReintegro).sum();
        long capacidadTotal = porSector.stream()
                .mapToLong(item -> item.capacidad() == null ? 0 : item.capacidad())
                .sum();

        return new ReporteEventoResponse(
                evento.getId(),
                evento.getEstado(),
                porSector,
                new TotalesEventoResponse(
                        emitidas,
                        activas,
                        devueltas,
                        enReintegro,
                        porcentaje(activas, capacidadTotal),
                        sumar(porSector, ReporteSectorEventoResponse::recaudacionBruta),
                        sumar(porSector, ReporteSectorEventoResponse::recaudacionDevuelta),
                        sumar(porSector, ReporteSectorEventoResponse::recaudacionNeta)
                )
        );
    }

    private static Long parsearIdUsuario(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new AccesoDenegadoException("X-User-Id es obligatorio para consultar el reporte");
        }
        try {
            Long id = Long.valueOf(valor.trim());
            if (id <= 0) {
                throw new AccesoDenegadoException("X-User-Id debe identificar un usuario válido");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new AccesoDenegadoException("X-User-Id debe ser numérico");
        }
    }

    private ReporteVentasEventoResponse consultarVentas(Long idEvento) {
        try {
            ReporteVentasEventoResponse ventas = entradaClient.consultarReporte(idEvento);
            if (ventas == null || !idEvento.equals(ventas.idEvento())
                    || ventas.porSector() == null || ventas.totales() == null) {
                throw new ServicioExternoException(
                        "Entrada devolvió un reporte incompleto para el evento " + idEvento, null);
            }
            return ventas;
        } catch (FeignException exception) {
            throw new ServicioExternoException(
                    "No se pudo consultar el reporte de ventas en msvc-entrada", exception);
        }
    }

    /** Incluye los sectores configurados sin ventas y cualquier sector con ventas del contrato interno. */
    private static Map<Long, ReporteSectorResponse> cifrasPorSector(
            Evento evento, ReporteVentasEventoResponse ventas) {
        Map<Long, ReporteSectorResponse> cifras = new LinkedHashMap<>();
        for (ReporteSectorResponse fila : ventas.porSector()) {
            if (fila != null && fila.idSector() != null) {
                cifras.put(fila.idSector(), fila);
            }
        }
        if (evento.getSectores() != null) {
            for (var sectorEvento : evento.getSectores()) {
                Long idSector = sectorEvento.getIdSector();
                if (idSector != null) {
                    cifras.putIfAbsent(idSector, sinMovimientos(idSector));
                }
            }
        }
        return new java.util.TreeMap<>(cifras);
    }

    private static ReporteSectorResponse sinMovimientos(Long idSector) {
        return new ReporteSectorResponse(idSector, 0, 0, 0, 0, CERO, CERO, CERO);
    }

    private ReporteSectorEventoResponse enriquecer(ReporteSectorResponse cifras) {
        Integer capacidad = 0;
        String nombreSector = null;
        try {
            SectorResponse sector = sectorClient.obtenerPorId(cifras.idSector());
            if (sector != null) {
                nombreSector = sector.nombre();
                capacidad = sector.capacidadMax() == null || sector.capacidadMax() <= 0
                        ? 0 : sector.capacidadMax();
            }
        } catch (FeignException.NotFound exception) {
            // El sector fue borrado en msvc-ubicacion: se conservan las cifras sin capacidad.
        } catch (FeignException exception) {
            throw new ServicioExternoException(
                    "No se pudo consultar el sector " + cifras.idSector() + " en msvc-ubicacion", exception);
        }

        return new ReporteSectorEventoResponse(
                cifras.idSector(),
                nombreSector,
                capacidad,
                cifras.entradasEmitidas(),
                cifras.entradasActivas(),
                cifras.entradasDevueltas(),
                cifras.enReintegro(),
                porcentaje(cifras.entradasActivas(), capacidad),
                cifras.recaudacionBruta(),
                cifras.recaudacionDevuelta(),
                cifras.recaudacionNeta()
        );
    }

    private static BigDecimal porcentaje(long activas, long capacidad) {
        if (capacidad <= 0) {
            return CERO;
        }
        return BigDecimal.valueOf(activas).multiply(CIEN)
                .divide(BigDecimal.valueOf(capacidad), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sumar(List<ReporteSectorEventoResponse> porSector,
                                    java.util.function.Function<ReporteSectorEventoResponse, BigDecimal> campo) {
        return porSector.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
