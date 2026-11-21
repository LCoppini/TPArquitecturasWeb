package com.ticketya.entrada.controller;

import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.exception.ApiExceptionHandler;
import com.ticketya.entrada.exception.QrError;
import com.ticketya.entrada.exception.QrExceptionHandler;
import com.ticketya.entrada.exception.QrRechazadoException;
import com.ticketya.entrada.service.EntradaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccesoControllerTest {

    @Mock
    private EntradaService entradaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AccesoController(entradaService))
                .setControllerAdvice(new QrExceptionHandler(), new ApiExceptionHandler())
                .build();
    }

    @Test
    void escaneoValidoDevuelveLaEntradaUsada() throws Exception {
        when(entradaService.validarQr("qr-1", 20L, 99L)).thenReturn(respuesta());

        mockMvc.perform(post("/api/accesos/escanear")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEvento\":20,\"codigoQr\":\"qr-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("USADA"));

        verify(entradaService).validarQr(eq("qr-1"), eq(20L), eq(99L));
    }

    @Test
    void codigoQrEnBlancoDevuelve400ConCodigo() throws Exception {
        mockMvc.perform(post("/api/accesos/escanear")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEvento\":20,\"codigoQr\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QR_REQUEST_INVALIDO"));

        verifyNoInteractions(entradaService);
    }

    @Test
    void idEventoAusenteDevuelve400ConCodigo() throws Exception {
        mockMvc.perform(post("/api/accesos/escanear")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoQr\":\"qr-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QR_REQUEST_INVALIDO"));

        verifyNoInteractions(entradaService);
    }

    @Test
    void rechazoDeDominioDevuelveEstadoYCodigo() throws Exception {
        when(entradaService.validarQr("qr-inexistente", 20L, 99L))
                .thenThrow(new QrRechazadoException(QrError.QR_NO_EXISTE));

        mockMvc.perform(post("/api/accesos/escanear")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEvento\":20,\"codigoQr\":\"qr-inexistente\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QR_NO_EXISTE"));
    }

    private EntradaResponse respuesta() {
        return new EntradaResponse(1L, 20L, 10L, 30L, null, null, BigDecimal.TEN, "qr-1", "USADA", null);
    }
}
