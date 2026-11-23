package com.ticketya.entrada.controller;

import com.ticketya.entrada.service.CancelacionEventoService;
import com.ticketya.entrada.service.CompraService;
import com.ticketya.entrada.service.EntradaService;
import com.ticketya.entrada.service.PagoService;
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
class AdminEntradaControllerCancelacionTest {

    @Mock private EntradaService entradaService;
    @Mock private PagoService pagoService;
    @Mock private CancelacionEventoService cancelacionEventoService;
    @Mock private CompraService compraService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminEntradaController(
                entradaService, pagoService, cancelacionEventoService, compraService
        )).build();
    }

    @Test
    void cancelarEventoUsaLaRutaAdministrativaYDevuelve202() throws Exception {
        mockMvc.perform(post("/api/admin/entradas/eventos/{idEvento}/cancelacion", 20L)
                        .header("X-User-Id", 5L))
                .andExpect(status().isAccepted());

        verify(cancelacionEventoService).cancelarEvento(20L);
    }

    @Test
    void cancelarEventoRequiereElHeaderXUserId() throws Exception {
        mockMvc.perform(post("/api/admin/entradas/eventos/{idEvento}/cancelacion", 20L))
                .andExpect(status().isBadRequest());
    }
}
