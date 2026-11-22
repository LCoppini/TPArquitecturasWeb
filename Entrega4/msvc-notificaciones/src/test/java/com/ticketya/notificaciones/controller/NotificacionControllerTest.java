package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.TipoNotificacion;
import com.ticketya.notificaciones.exception.NotificacionesExceptionHandler;
import com.ticketya.notificaciones.exception.RecursoNoEncontradoException;
import com.ticketya.notificaciones.exception.SolicitudInvalidaException;
import com.ticketya.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private static final String SOLICITUD_VALIDA = """
            {
              "claveIdempotencia": "COMPRA_CONFIRMADA:idEntrada:101",
              "idUsuario": 42,
              "tipo": "COMPRA_CONFIRMADA",
              "datos": {"evento": "Ritual en vivo", "fechaHora": "24/10 21:00", "idEntrada": "101"}
            }
            """;

    @Mock
    private NotificacionService notificacionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificacionController(notificacionService))
                .setControllerAdvice(new NotificacionesExceptionHandler())
                .build();
    }

    private static NotificacionResponse respuesta(EstadoNotificacion estado) {
        return new NotificacionResponse(7L, "COMPRA_CONFIRMADA:idEntrada:101", 42L, TipoNotificacion.COMPRA_CONFIRMADA,
                null, null, estado, 0, null, null, null, null, null);
    }

    @Test
    void solicitudValidaResponde202ConLaNotificacionPendiente() throws Exception {
        when(notificacionService.solicitar(any())).thenReturn(respuesta(EstadoNotificacion.PENDIENTE));

        mockMvc.perform(post("/internal/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void solicitudSinClaveResponde400IndicandoElCampo() throws Exception {
        mockMvc.perform(post("/internal/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\": 42, \"tipo\": \"COMPRA_CONFIRMADA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("claveIdempotencia")));
        verifyNoInteractions(notificacionService);
    }

    @Test
    void emailInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/internal/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveIdempotencia\": \"k1\", \"idUsuario\": 42, "
                                + "\"tipo\": \"COMPRA_CONFIRMADA\", \"destinatario\": \"no-es-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("destinatario")));
    }

    @Test
    void tipoDesconocidoResponde400() throws Exception {
        mockMvc.perform(post("/internal/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claveIdempotencia\": \"k1\", \"idUsuario\": 42, \"tipo\": \"INVENTADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void faltanDatosDelTipoResponde400ConElDetalle() throws Exception {
        when(notificacionService.solicitar(any()))
                .thenThrow(new SolicitudInvalidaException("Faltan datos obligatorios para COMPRA_CONFIRMADA: idEntrada"));

        mockMvc.perform(post("/internal/notificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("idEntrada")))
                .andExpect(jsonPath("$.ruta").value("/internal/notificaciones"));
    }

    @Test
    void notificacionInexistenteResponde404() throws Exception {
        when(notificacionService.obtenerPorId(9L)).thenThrow(new RecursoNoEncontradoException("La notificacion 9 no existe"));

        mockMvc.perform(get("/internal/notificaciones/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La notificacion 9 no existe"));
    }

    @Test
    void consultaPorIdDevuelveElEstado() throws Exception {
        when(notificacionService.obtenerPorId(7L)).thenReturn(respuesta(EstadoNotificacion.ENVIADA));

        mockMvc.perform(get("/internal/notificaciones/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADA"));
    }

    @Test
    void historialFiltraPorUsuarioYEstado() throws Exception {
        when(notificacionService.listar(42L, EstadoNotificacion.FALLIDA))
                .thenReturn(List.of(respuesta(EstadoNotificacion.FALLIDA)));

        mockMvc.perform(get("/internal/notificaciones")
                        .param("idUsuario", "42")
                        .param("estado", "FALLIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("FALLIDA"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void estadoDesconocidoEnElHistorialResponde400() throws Exception {
        mockMvc.perform(get("/internal/notificaciones").param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(notificacionService);
    }
}
