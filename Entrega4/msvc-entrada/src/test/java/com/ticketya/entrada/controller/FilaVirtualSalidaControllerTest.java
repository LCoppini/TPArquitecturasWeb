package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.response.FilaEventoResponse;
import com.ticketya.entrada.dto.response.IngresoFilaResponse;
import com.ticketya.entrada.exception.ApiExceptionHandler;
import com.ticketya.entrada.service.FilaVirtualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FilaVirtualSalidaControllerTest {

    @Mock
    private FilaVirtualService filaVirtualService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FilaVirtualController(filaVirtualService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void salirUsaElIdUsuarioDelHeaderYDevuelveElEstado() throws Exception {
        when(filaVirtualService.salir(20L, 10L)).thenReturn(new IngresoFilaResponse(
                20L, 100L, 7L, "SALIO", null, null, null));

        mockMvc.perform(delete("/api/filas-virtuales/eventos/{idEvento}/ingresos/mi", 20L)
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SALIO"));

        verify(filaVirtualService).salir(20L, 10L);
    }

    @Test
    void salirRequiereElHeaderXUserId() throws Exception {
        mockMvc.perform(delete("/api/filas-virtuales/eventos/{idEvento}/ingresos/mi", 20L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarFilaDevuelveElEstadoDeLaFilaDelEvento() throws Exception {
        when(filaVirtualService.consultarFila(20L)).thenReturn(new FilaEventoResponse(20L, "ACTIVA", 5, 10));

        mockMvc.perform(get("/api/filas-virtuales/eventos/{idEvento}", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.maxPersonas").value(5));
    }

    @Test
    void consultarFilaDeEventoSinFilaDevuelve404() throws Exception {
        when(filaVirtualService.consultarFila(21L))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("El evento no tiene una fila persistida"));

        mockMvc.perform(get("/api/filas-virtuales/eventos/{idEvento}", 21L))
                .andExpect(status().isNotFound());
    }
}
