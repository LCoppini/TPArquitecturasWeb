package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.response.ReporteSectorResponse;
import com.ticketya.entrada.dto.response.ReporteVentasEventoResponse;
import com.ticketya.entrada.dto.response.TotalesReporteResponse;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cifras agregadas de ventas y dinero por evento. Las consultas agrupan en base
 * de datos por (sector, estado) y por (sector, estado del pago); el servicio
 * solo combina los renglones y aplica las reglas de negocio.
 */
@Service
@Transactional(readOnly = true)
public class ReporteVentasService {

    private static final List<String> ESTADOS_ENTRADA =
            List.of("VALIDA", "USADA", "EN_REINTEGRO", "DEVUELTA");
    private static final List<String> ESTADOS_PAGO =
            List.of("APROBADO", "REINTEGRO_PENDIENTE", "REEMBOLSADO");

    private final EntradaRepository entradaRepository;
    private final RegistroPagoRepository registroPagoRepository;

    public ReporteVentasService(EntradaRepository entradaRepository,
                                RegistroPagoRepository registroPagoRepository) {
        this.entradaRepository = entradaRepository;
        this.registroPagoRepository = registroPagoRepository;
    }

    public ReporteVentasEventoResponse reporte(Long idEvento) {
        Map<Long, Acumulado> acumulados = new TreeMap<>();

        for (EntradaRepository.ConteoPorSectorYEstado fila
                : entradaRepository.contarPorSectorYEstado(idEvento, ESTADOS_ENTRADA)) {
            Acumulado acumulado = acumulados.computeIfAbsent(fila.getIdSector(), clave -> new Acumulado());
            long cantidad = fila.getCantidad() == null ? 0 : fila.getCantidad();
            switch (fila.getEstado()) {
                case "VALIDA", "USADA" -> {
                    acumulado.entradasEmitidas += cantidad;
                    acumulado.entradasActivas += cantidad;
                }
                case "DEVUELTA" -> {
                    acumulado.entradasEmitidas += cantidad;
                    acumulado.entradasDevueltas += cantidad;
                }
                case "EN_REINTEGRO" -> {
                    acumulado.entradasEmitidas += cantidad;
                    acumulado.enReintegro += cantidad;
                }
                default -> { /* reservadas, vencidas y canceladas no entran en el histórico */ }
            }
        }

        for (RegistroPagoRepository.RecaudacionPorSectorYEstadoPago fila
                : registroPagoRepository.recaudarPorSectorYEstadoPago(idEvento, ESTADOS_PAGO)) {
            Acumulado acumulado = acumulados.computeIfAbsent(fila.getIdSector(), clave -> new Acumulado());
            BigDecimal monto = fila.getMonto() == null ? BigDecimal.ZERO : fila.getMonto();
            acumulado.recaudacionBruta = acumulado.recaudacionBruta.add(monto);
            if ("REEMBOLSADO".equals(fila.getEstadoPago())) {
                acumulado.recaudacionDevuelta = acumulado.recaudacionDevuelta.add(monto);
            }
        }

        List<ReporteSectorResponse> porSector = acumulados.entrySet().stream()
                .map(entry -> aRespuesta(entry.getKey(), entry.getValue()))
                .toList();

        return new ReporteVentasEventoResponse(idEvento, porSector, totales(porSector));
    }

    private static ReporteSectorResponse aRespuesta(Long idSector, Acumulado acumulado) {
        BigDecimal bruta = redondear(acumulado.recaudacionBruta);
        BigDecimal devuelta = redondear(acumulado.recaudacionDevuelta);
        return new ReporteSectorResponse(
                idSector,
                acumulado.entradasEmitidas,
                acumulado.entradasActivas,
                acumulado.entradasDevueltas,
                acumulado.enReintegro,
                bruta,
                devuelta,
                redondear(bruta.subtract(devuelta))
        );
    }

    private static TotalesReporteResponse totales(List<ReporteSectorResponse> porSector) {
        BigDecimal bruta = BigDecimal.ZERO;
        BigDecimal devuelta = BigDecimal.ZERO;
        for (ReporteSectorResponse sector : porSector) {
            bruta = bruta.add(sector.recaudacionBruta());
            devuelta = devuelta.add(sector.recaudacionDevuelta());
        }
        return new TotalesReporteResponse(
                porSector.stream().mapToLong(ReporteSectorResponse::entradasEmitidas).sum(),
                porSector.stream().mapToLong(ReporteSectorResponse::entradasActivas).sum(),
                porSector.stream().mapToLong(ReporteSectorResponse::entradasDevueltas).sum(),
                porSector.stream().mapToLong(ReporteSectorResponse::enReintegro).sum(),
                redondear(bruta),
                redondear(devuelta),
                redondear(bruta.subtract(devuelta))
        );
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class Acumulado {
        private long entradasEmitidas;
        private long entradasActivas;
        private long entradasDevueltas;
        private long enReintegro;
        private BigDecimal recaudacionBruta = BigDecimal.ZERO;
        private BigDecimal recaudacionDevuelta = BigDecimal.ZERO;
    }
}
