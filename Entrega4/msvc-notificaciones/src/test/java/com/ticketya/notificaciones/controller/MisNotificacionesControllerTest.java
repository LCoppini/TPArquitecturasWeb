package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MisNotificacionesControllerTest {

    private NotificacionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(NotificacionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MisNotificacionesController(service)).build();
    }

    @Test
    void devuelveSoloLasDelUsuarioDelHeaderSinDatosInternos() throws Exception {
        when(service.listar(42L, null)).thenReturn(List.of(new NotificacionResponse(
                7L, "COMPRA_CONFIRMADA:idEntrada:101", 42L, TipoNotificacion.COMPRA_CONFIRMADA, "ana@correo.com",
                "Tu compra esta confirmada", EstadoNotificacion.FALLIDA, 3, null, "Casilla llena: 550", "corr-1",
                Instant.parse("2026-11-22T21:53:00Z"), null)));

        mockMvc.perform(get("/api/notificaciones/mias").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].tipo").value("COMPRA_CONFIRMADA"))
                .andExpect(jsonPath("$[0].estado").value("FALLIDA"))
                .andExpect(jsonPath("$[0].asunto").value("Tu compra esta confirmada"))
                .andExpect(jsonPath("$[0].ultimoError").doesNotExist())
                .andExpect(jsonPath("$[0].claveIdempotencia").doesNotExist())
                .andExpect(jsonPath("$[0].correlationId").doesNotExist());
        verify(service).listar(42L, null);
    }

    @Test
    void sinHeaderDeIdentidadEsSolicitudInvalida() throws Exception {
        mockMvc.perform(get("/api/notificaciones/mias")).andExpect(status().is4xxClientError());
    }
}
