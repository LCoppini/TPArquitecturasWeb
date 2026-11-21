package com.ticketya.entrada.controller;

import com.ticketya.entrada.service.EntradaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalDisponibilidadControllerTest {

    private EntradaService entradaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        entradaService = mock(EntradaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalDisponibilidadController(entradaService)).build();
    }

    @Test
    void disponibilidadInternaDevuelveLosCincoCampos() throws Exception {
        when(entradaService.disponibilidad(1L, 2L)).thenReturn(Map.of(
                "idEvento", 1L,
                "idSector", 2L,
                "capacidad", 100,
                "ocupadas", 40L,
                "disponibles", 60L
        ));

        mockMvc.perform(get("/internal/entradas/disponibilidad")
                        .param("idEvento", "1")
                        .param("idSector", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(1))
                .andExpect(jsonPath("$.idSector").value(2))
                .andExpect(jsonPath("$.capacidad").value(100))
                .andExpect(jsonPath("$.ocupadas").value(40))
                .andExpect(jsonPath("$.disponibles").value(60));

        verify(entradaService).disponibilidad(1L, 2L);
    }
}
