package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.response.ReporteSectorResponse;
import com.ticketya.entrada.dto.response.ReporteVentasEventoResponse;
import com.ticketya.entrada.dto.response.TotalesReporteResponse;
import com.ticketya.entrada.service.ReporteVentasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalReporteControllerTest {

    private ReporteVentasService reporteVentasService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reporteVentasService = mock(ReporteVentasService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalReporteController(reporteVentasService)).build();
    }

    @Test
    void reporteInternoDevuelveElContratoAgrupadoSinDatosDeEvento() throws Exception {
        when(reporteVentasService.reporte(10L)).thenReturn(new ReporteVentasEventoResponse(
                10L,
                List.of(new ReporteSectorResponse(1L, 127, 120, 5, 2,
                        new BigDecimal("127000.00"), new BigDecimal("5000.00"), new BigDecimal("122000.00"))),
                new TotalesReporteResponse(127, 120, 5, 2,
                        new BigDecimal("127000.00"), new BigDecimal("5000.00"), new BigDecimal("122000.00"))
        ));

        mockMvc.perform(get("/internal/entradas/eventos/10/reporte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(10))
                .andExpect(jsonPath("$.porSector[0].idSector").value(1))
                .andExpect(jsonPath("$.porSector[0].entradasActivas").value(120))
                .andExpect(jsonPath("$.porSector[0].recaudacionNeta").value(122000.00))
                .andExpect(jsonPath("$.totales.entradasEmitidas").value(127))
                .andExpect(jsonPath("$.totales.recaudacionBruta").value(127000.00))
                .andExpect(jsonPath("$.estadoEvento").doesNotExist())
                .andExpect(jsonPath("$.capacidad").doesNotExist());

        verify(reporteVentasService).reporte(10L);
    }
}
