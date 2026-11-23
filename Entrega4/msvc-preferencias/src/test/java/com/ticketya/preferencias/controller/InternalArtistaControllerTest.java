package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.ClientesDeArtistaResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalArtistaControllerTest {

    private PreferenciaService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PreferenciaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalArtistaController(service)).build();
    }

    @Test
    void devuelveLosClientesDelArtistaEnLaCiudad() throws Exception {
        when(service.clientesPorArtistaYCiudad(5L, "Tandil"))
                .thenReturn(new ClientesDeArtistaResponse(5L, "Tandil", List.of(3L, 4L)));

        mockMvc.perform(get("/internal/preferencias/artistas/{id}/clientes", 5L).param("ciudad", "Tandil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idArtista").value(5))
                .andExpect(jsonPath("$.idsUsuarios[0]").value(3))
                .andExpect(jsonPath("$.idsUsuarios[1]").value(4));
    }

    @Test
    void sinCiudadResponde400() throws Exception {
        mockMvc.perform(get("/internal/preferencias/artistas/{id}/clientes", 5L))
                .andExpect(status().isBadRequest());
    }
}
