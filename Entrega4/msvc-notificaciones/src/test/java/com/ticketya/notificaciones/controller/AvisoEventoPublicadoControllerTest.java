package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.response.EventoPublicadoResponse;
import com.ticketya.notificaciones.exception.NotificacionesExceptionHandler;
import com.ticketya.notificaciones.service.AvisoEventoPublicadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvisoEventoPublicadoControllerTest {

    private AvisoEventoPublicadoService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AvisoEventoPublicadoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AvisoEventoPublicadoController(service))
                .setControllerAdvice(new NotificacionesExceptionHandler())
                .build();
    }

    @Test
    void solicitudValidaResponde202() throws Exception {
        when(service.avisar(any())).thenReturn(new EventoPublicadoResponse(7L, 2));

        mockMvc.perform(post("/internal/notificaciones/eventos-publicados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEvento":7,"evento":"Copa Ciudad","fechaHora":"10/01/2027 18:00","ciudad":"Tandil",
                                 "artistas":[{"idArtista":1,"nombre":"Banda A"}]}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.usuariosNotificados").value(2));
    }

    @Test
    void sinArtistasResponde400() throws Exception {
        mockMvc.perform(post("/internal/notificaciones/eventos-publicados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idEvento":7,"evento":"Copa Ciudad","fechaHora":"10/01/2027 18:00","ciudad":"Tandil",
                                 "artistas":[]}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
