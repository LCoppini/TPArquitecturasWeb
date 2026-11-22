package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.response.ReporteSectorResponse;
import com.ticketya.entrada.dto.response.ReporteVentasEventoResponse;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteVentasServiceTest {

    private static final List<String> ESTADOS_ENTRADA =
            List.of("VALIDA", "USADA", "EN_REINTEGRO", "DEVUELTA");
    private static final List<String> ESTADOS_PAGO =
            List.of("APROBADO", "REINTEGRO_PENDIENTE", "REEMBOLSADO");

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private RegistroPagoRepository registroPagoRepository;

    private ReporteVentasService service;

    @BeforeEach
    void setUp() {
        service = new ReporteVentasService(entradaRepository, registroPagoRepository);
    }

    private static EntradaRepository.ConteoPorSectorYEstado conteo(long idSector, String estado, long cantidad) {
        return new EntradaRepository.ConteoPorSectorYEstado() {
            @Override
            public Long getIdSector() {
                return idSector;
            }

            @Override
            public String getEstado() {
                return estado;
            }

            @Override
            public Long getCantidad() {
                return cantidad;
            }
        };
    }

    private static RegistroPagoRepository.RecaudacionPorSectorYEstadoPago pago(
            long idSector, String estadoPago, String monto) {
        return new RegistroPagoRepository.RecaudacionPorSectorYEstadoPago() {
            @Override
            public Long getIdSector() {
                return idSector;
            }

            @Override
            public String getEstadoPago() {
                return estadoPago;
            }

            @Override
            public BigDecimal getMonto() {
                return new BigDecimal(monto);
            }
        };
    }

    private void esperarConteos(EntradaRepository.ConteoPorSectorYEstado... filas) {
        when(entradaRepository.contarPorSectorYEstado(10L, ESTADOS_ENTRADA)).thenReturn(List.of(filas));
    }

    private void esperarPagos(RegistroPagoRepository.RecaudacionPorSectorYEstadoPago... filas) {
        when(registroPagoRepository.recaudarPorSectorYEstadoPago(10L, ESTADOS_PAGO)).thenReturn(List.of(filas));
    }

    @Test
    void agrupaPorSectorYEstadoYRespetaLasReglasDeConteo() {
        esperarConteos(
                conteo(1, "VALIDA", 100),
                conteo(1, "USADA", 20),
                conteo(1, "DEVUELTA", 5),
                conteo(1, "EN_REINTEGRO", 2),
                conteo(1, "RESERVADA", 9),
                conteo(2, "VALIDA", 3)
        );
        esperarPagos(
                pago(1, "APROBADO", "1000"),
                pago(1, "REEMBOLSADO", "50"),
                pago(1, "REINTEGRO_PENDIENTE", "25"),
                pago(2, "APROBADO", "30")
        );

        ReporteVentasEventoResponse respuesta = service.reporte(10L);

        assertEquals(10L, respuesta.idEvento());
        assertEquals(2, respuesta.porSector().size());
        ReporteSectorResponse sector1 = respuesta.porSector().getFirst();
        assertEquals(1L, sector1.idSector());
        assertEquals(127, sector1.entradasEmitidas());
        assertEquals(120, sector1.entradasActivas());
        assertEquals(5, sector1.entradasDevueltas());
        assertEquals(2, sector1.enReintegro());
        assertEquals(dinero("1075.00"), sector1.recaudacionBruta());
        assertEquals(dinero("50.00"), sector1.recaudacionDevuelta());
        assertEquals(dinero("1025.00"), sector1.recaudacionNeta());

        ReporteSectorResponse sector2 = respuesta.porSector().get(1);
        assertEquals(2L, sector2.idSector());
        assertEquals(3, sector2.entradasActivas());
        assertEquals(dinero("30.00"), sector2.recaudacionNeta());

        assertEquals(130, respuesta.totales().entradasEmitidas());
        assertEquals(123, respuesta.totales().entradasActivas());
        assertEquals(5, respuesta.totales().entradasDevueltas());
        assertEquals(2, respuesta.totales().enReintegro());
        assertEquals(dinero("1105.00"), respuesta.totales().recaudacionBruta());
        assertEquals(dinero("50.00"), respuesta.totales().recaudacionDevuelta());
        assertEquals(dinero("1055.00"), respuesta.totales().recaudacionNeta());
    }

    @Test
    void usaUnaSolaConsultaAgrupadaPorEstadoYNoUnaPorCadaEstado() {
        esperarConteos(conteo(1, "VALIDA", 1));
        esperarPagos(pago(1, "APROBADO", "10"));

        service.reporte(10L);

        verify(entradaRepository, times(1)).contarPorSectorYEstado(10L, ESTADOS_ENTRADA);
        verify(registroPagoRepository, times(1)).recaudarPorSectorYEstadoPago(10L, ESTADOS_PAGO);
    }

    @Test
    void dosPagosAprobadosDelMismoCompradorSumanDosRegistros() {
        esperarConteos(conteo(1, "VALIDA", 2));
        esperarPagos(pago(1, "APROBADO", "1000"), pago(1, "APROBADO", "1000"));

        ReporteVentasEventoResponse respuesta = service.reporte(10L);

        assertEquals(dinero("2000.00"), respuesta.totales().recaudacionBruta());
        assertEquals(dinero("0.00"), respuesta.totales().recaudacionDevuelta());
        assertEquals(dinero("2000.00"), respuesta.totales().recaudacionNeta());
    }

    @Test
    void reintegroPendienteSigueContandoEnBrutaYEnNeta() {
        esperarConteos(conteo(1, "EN_REINTEGRO", 1));
        esperarPagos(pago(1, "REINTEGRO_PENDIENTE", "1000"));

        ReporteVentasEventoResponse respuesta = service.reporte(10L);

        assertEquals(1, respuesta.totales().enReintegro());
        assertEquals(dinero("1000.00"), respuesta.totales().recaudacionBruta());
        assertEquals(dinero("0.00"), respuesta.totales().recaudacionDevuelta());
        assertEquals(dinero("1000.00"), respuesta.totales().recaudacionNeta());
    }

    @Test
    void sinMovimientosDevuelvePorSectorVacioYTotalesEnCero() {
        esperarConteos();
        esperarPagos();

        ReporteVentasEventoResponse respuesta = service.reporte(10L);

        assertTrue(respuesta.porSector().isEmpty());
        assertEquals(0, respuesta.totales().entradasEmitidas());
        assertEquals(dinero("0.00"), respuesta.totales().recaudacionBruta());
        assertEquals(dinero("0.00"), respuesta.totales().recaudacionDevuelta());
        assertEquals(dinero("0.00"), respuesta.totales().recaudacionNeta());
    }

    @Test
    void redondeaImportesConHalfUpADosDecimales() {
        esperarConteos(conteo(1, "VALIDA", 1));
        esperarPagos(pago(1, "APROBADO", "10.005"), pago(1, "REEMBOLSADO", "0.005"));

        ReporteVentasEventoResponse respuesta = service.reporte(10L);

        assertEquals(dinero("10.01"), respuesta.totales().recaudacionBruta());
        assertEquals(dinero("0.01"), respuesta.totales().recaudacionDevuelta());
        assertEquals(dinero("10.00"), respuesta.totales().recaudacionNeta());
    }

    @Test
    void noDobleDescuentaConDatosArmadosManualmente() {
        // Fixture artificial: con FALLA el flujo real deja la entrada en EN_REINTEGRO,
        // por lo que DEVUELTA + REINTEGRO_PENDIENTE no es alcanzable desde cancelarEvento().
        esperarConteos(conteo(1, "DEVUELTA", 1));
        esperarPagos(pago(1, "REINTEGRO_PENDIENTE", "1000"));

        ReporteVentasEventoResponse respuesta = service.reporte(10L);

        assertEquals(1, respuesta.totales().entradasDevueltas());
        assertEquals(0, respuesta.totales().enReintegro());
        assertEquals(dinero("1000.00"), respuesta.totales().recaudacionBruta());
        assertEquals(dinero("0.00"), respuesta.totales().recaudacionDevuelta());
        assertEquals(dinero("1000.00"), respuesta.totales().recaudacionNeta());
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }
}
