package com.ticketya.entrada.controller;

import com.ticketya.entrada.exception.ApiExceptionHandler;
import com.ticketya.entrada.service.ModificacionEventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalEventoModificadoControllerTest {

    @Mock
    private ModificacionEventoService servicio;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalEventoModificadoController(servicio))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void aceptaLosCambiosYLosDelegaAlServicio() throws Exception {
        mockMvc.perform(post("/internal/entradas/eventos/{idEvento}/modificacion", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cambios\":\"fecha: 10/01/2027 21:00 a 12/01/2027 21:00\"}"))
                .andExpect(status().isAccepted());

        verify(servicio).notificarModificacion(20L, "fecha: 10/01/2027 21:00 a 12/01/2027 21:00");
    }

    @Test
    void rechazaCambiosVacios() throws Exception {
        mockMvc.perform(post("/internal/entradas/eventos/{idEvento}/modificacion", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cambios\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(servicio, never()).notificarModificacion(any(), anyString());
    }
}
