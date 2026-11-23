package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.request.ActivarFilaRequest;
import com.ticketya.entrada.dto.response.FilaEventoResponse;
import com.ticketya.entrada.dto.response.IngresoFilaResponse;
import com.ticketya.entrada.exception.ApiExceptionHandler;
import com.ticketya.entrada.service.FilaVirtualService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FilaVirtualControllerTest {

    @Mock
    private FilaVirtualService filaVirtualService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new FilaVirtualController(filaVirtualService),
                        new AdminFilaVirtualController(filaVirtualService)
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    @Test
    void ingresoUsaElIdUsuarioDelHeaderYDevuelveLaPosicion() throws Exception {
        when(filaVirtualService.unirse(20L, 10L)).thenReturn(new IngresoFilaResponse(
                20L, 100L, 7L, "EN_ESPERA", 3L, null, null
        ));

        mockMvc.perform(post("/api/filas-virtuales/eventos/{idEvento}/ingresos", 20L)
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(20))
                .andExpect(jsonPath("$.secuencia").value(7))
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"))
                .andExpect(jsonPath("$.posicion").value(3));

        verify(filaVirtualService).unirse(20L, 10L);
    }

    @Test
    void ingresoRequiereElHeaderXUserId() throws Exception {
        mockMvc.perform(post("/api/filas-virtuales/eventos/{idEvento}/ingresos", 20L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarMiEstadoDevuelveElTurnoVigente() throws Exception {
        Instant venceEn = Instant.parse("2026-11-22T12:00:00Z");
        when(filaVirtualService.consultarMiEstado(20L, 10L)).thenReturn(new IngresoFilaResponse(
                20L, 100L, 1L, "HABILITADO", 0L, venceEn, 200L
        ));

        mockMvc.perform(get("/api/filas-virtuales/eventos/{idEvento}/mi-estado", 20L)
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("HABILITADO"))
                .andExpect(jsonPath("$.idTanda").value(200))
                .andExpect(jsonPath("$.venceEn").value("2026-11-22T12:00:00Z"));
    }

    @Test
    void activacionConBodyPasaLosParametrosAlServicio() throws Exception {
        when(filaVirtualService.activar(20L, 10L, new ActivarFilaRequest(3, 6)))
                .thenReturn(new FilaEventoResponse(20L, "ACTIVA", 3, 6));

        mockMvc.perform(put("/api/organizador/filas-virtuales/eventos/{idEvento}/activacion", 20L)
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxPersonas\":3,\"cupoEntradas\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.maxPersonas").value(3))
                .andExpect(jsonPath("$.cupoEntradas").value(6));

        ArgumentCaptor<ActivarFilaRequest> captor = ArgumentCaptor.forClass(ActivarFilaRequest.class);
        verify(filaVirtualService).activar(eq(20L), eq(10L), captor.capture());
        assertEquals(3, captor.getValue().maxPersonas());
        assertEquals(6, captor.getValue().cupoEntradas());
    }

    @Test
    void activacionSinBodyUsaLosDefaultsDelServicio() throws Exception {
        when(filaVirtualService.activar(20L, 10L, null))
                .thenReturn(new FilaEventoResponse(20L, "ACTIVA", 5, 10));

        mockMvc.perform(put("/api/organizador/filas-virtuales/eventos/{idEvento}/activacion", 20L)
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPersonas").value(5));

        verify(filaVirtualService).activar(20L, 10L, null);
    }

    @Test
    void activacionRequiereElHeaderXUserId() throws Exception {
        mockMvc.perform(put("/api/organizador/filas-virtuales/eventos/{idEvento}/activacion", 20L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administrarPropagaLaOperacionAlServicio() throws Exception {
        when(filaVirtualService.administrar(20L, 10L, "pausa"))
                .thenReturn(new FilaEventoResponse(20L, "PAUSADA", 3, 6));

        mockMvc.perform(post("/api/organizador/filas-virtuales/eventos/{idEvento}/{operacion}", 20L, "pausa")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAUSADA"));

        verify(filaVirtualService).administrar(20L, 10L, "pausa");
    }

    @Test
    void ausenciaDeFilaSePropagaComo404() throws Exception {
        when(filaVirtualService.consultarMiEstado(20L, 10L))
                .thenThrow(new EntityNotFoundException("El evento no tiene una fila persistida"));

        mockMvc.perform(get("/api/filas-virtuales/eventos/{idEvento}/mi-estado", 20L)
                        .header("X-User-Id", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void transicionNoPermitidaSePropagaComo409() throws Exception {
        when(filaVirtualService.administrar(20L, 10L, "reanudar"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La fila cerrada no se puede reanudar"));

        mockMvc.perform(post("/api/organizador/filas-virtuales/eventos/{idEvento}/{operacion}", 20L, "reanudar")
                        .header("X-User-Id", 10L))
                .andExpect(status().isConflict());
    }

    @Test
    void eventoNoDisponibleSePropagaComo503() throws Exception {
        when(filaVirtualService.activar(20L, 10L, null))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar msvc-evento"));

        mockMvc.perform(put("/api/organizador/filas-virtuales/eventos/{idEvento}/activacion", 20L)
                        .header("X-User-Id", 10L))
                .andExpect(status().isServiceUnavailable());
    }
}
