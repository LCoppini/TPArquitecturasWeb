package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.RecomendacionResponse;
import com.ticketya.preferencias.service.RecomendacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecomendacionControllerTest {

    private RecomendacionService service;
    private MockMvc clienteMvc;
    private MockMvc adminMvc;

    @BeforeEach
    void setUp() {
        service = mock(RecomendacionService.class);
        clienteMvc = MockMvcBuilders.standaloneSetup(new RecomendacionController(service)).build();
        adminMvc = MockMvcBuilders.standaloneSetup(new RecomendacionAdminController(service)).build();
    }

    @Test
    void clienteConsultaSusRecomendaciones() throws Exception {
        when(service.recomendar(42L)).thenReturn(List.of(new RecomendacionResponse(
                20L, Instant.parse("2026-12-01T20:00:00Z"), "SHOW", "MISMO_TIPO")));

        clienteMvc.perform(get("/api/recomendaciones/clientes/42").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEvento").value(20L))
                .andExpect(jsonPath("$[0].motivo").value("MISMO_TIPO"))
                .andExpect(jsonPath("$[0].tipo").value("SHOW"));
    }

    @Test
    void clienteNoPuedeConsultarRecomendacionesDeOtro() throws Exception {
        clienteMvc.perform(get("/api/recomendaciones/clientes/42").header("X-User-Id", "99"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void administradorConsultaRecomendacionesDeCualquierCliente() throws Exception {
        when(service.recomendar(42L)).thenReturn(List.of());

        adminMvc.perform(get("/api/admin/recomendaciones/clientes/42"))
                .andExpect(status().isOk());

        verify(service).recomendar(42L);
    }

    @Test
    void administradorNoComparaElHeader() throws Exception {
        when(service.recomendar(42L)).thenReturn(List.of());

        adminMvc.perform(get("/api/admin/recomendaciones/clientes/42").header("X-User-Id", "99"))
                .andExpect(status().isOk());

        verify(service).recomendar(42L);
    }
}
