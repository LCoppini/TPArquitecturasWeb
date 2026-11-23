package com.ticketya.evento.controller;

import com.ticketya.evento.service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminEventoControllerCancelacionTest {

    @Mock private EventoService eventoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminEventoController(eventoService)).build();
    }

    @Test
    void reintentarCoordinacionUsaLaRutaAdministrativaYDelegaEnElServicio() throws Exception {
        mockMvc.perform(post("/api/admin/eventos/{id}/cancelacion/reintentar", 20L)
                        .header("X-User-Id", 5L))
                .andExpect(status().isOk());

        verify(eventoService).reintentarCoordinacionCancelacion(20L, 5L);
    }

    @Test
    void reintentarCoordinacionRequiereElHeaderXUserId() throws Exception {
        mockMvc.perform(post("/api/admin/eventos/{id}/cancelacion/reintentar", 20L))
                .andExpect(status().isBadRequest());
    }
}
