package com.ticketya.evento.controller;

import com.ticketya.evento.dto.response.ReporteEventoResponse;
import com.ticketya.evento.dto.response.TotalesEventoResponse;
import com.ticketya.evento.exception.AccesoDenegadoException;
import com.ticketya.evento.exception.EventoExceptionHandler;
import com.ticketya.evento.service.EventoService;
import com.ticketya.evento.service.ReporteOrganizadorService;
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

class OrganizadorEventoControllerTest {

    private ReporteOrganizadorService reporteOrganizadorService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reporteOrganizadorService = mock(ReporteOrganizadorService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrganizadorEventoController(mock(EventoService.class), reporteOrganizadorService))
                .setControllerAdvice(new EventoExceptionHandler())
                .build();
    }

    @Test
    void reportePasaElHeaderComoTextoParaValidarloEnElServicio() throws Exception {
        when(reporteOrganizadorService.reporte(10L, "42")).thenReturn(new ReporteEventoResponse(
                10L, "PUBLICADO", List.of(),
                new TotalesEventoResponse(0, 0, 0, 0, new BigDecimal("0.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        ));

        mockMvc.perform(get("/api/organizador/eventos/10/reporte").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(10))
                .andExpect(jsonPath("$.estadoEvento").value("PUBLICADO"));

        verify(reporteOrganizadorService).reporte(10L, "42");
    }

    @Test
    void idEventoNoNumericoResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(get("/api/organizador/eventos/abc/reporte").header("X-User-Id", "42"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void identidadInvalidaDelHeaderSeMantieneComo403() throws Exception {
        when(reporteOrganizadorService.reporte(10L, "abc"))
                .thenThrow(new AccesoDenegadoException("X-User-Id debe ser numérico"));

        mockMvc.perform(get("/api/organizador/eventos/10/reporte").header("X-User-Id", "abc"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }
}
