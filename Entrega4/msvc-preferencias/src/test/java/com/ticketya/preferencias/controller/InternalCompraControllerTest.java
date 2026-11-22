package com.ticketya.preferencias.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.preferencias.dto.request.ArtistaCompraRequest;
import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.dto.response.CompraRegistradaResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalCompraControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PreferenciaService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PreferenciaService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalCompraController(service))
                .setValidator(validator)
                .build();
    }

    @Test
    void primeraCompraDevuelve201() throws Exception {
        when(service.registrarCompra(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CompraRegistradaResponse(101L, false));

        mockMvc.perform(post("/internal/preferencias/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra("Tandil"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEntrada").value(101L))
                .andExpect(jsonPath("$.yaProcesada").value(false));
    }

    @Test
    void reintentoConLaMismaEntradaDevuelve200() throws Exception {
        when(service.registrarCompra(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CompraRegistradaResponse(101L, true));

        mockMvc.perform(post("/internal/preferencias/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compra("Tandil"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yaProcesada").value(true));
    }

    @Test
    void contratoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/internal/preferencias/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoEvento\":\"SHOW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elControllerPasaElContratoCompletoAlServicio() throws Exception {
        when(service.registrarCompra(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CompraRegistradaResponse(101L, false));
        CompraRegistradaRequest request = new CompraRegistradaRequest(42L, 101L, 20L, "SHOW", "Tandil",
                List.of(new ArtistaCompraRequest(7L, "Banda")));

        mockMvc.perform(post("/internal/preferencias/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ArgumentCaptor<CompraRegistradaRequest> captor = ArgumentCaptor.forClass(CompraRegistradaRequest.class);
        verify(service).registrarCompra(captor.capture());
        assertEquals(42L, captor.getValue().idUsuario());
        assertEquals(101L, captor.getValue().idEntrada());
        assertEquals(20L, captor.getValue().idEvento());
        assertEquals("SHOW", captor.getValue().tipoEvento());
        assertEquals("Tandil", captor.getValue().ciudad());
        assertEquals(7L, captor.getValue().artistas().get(0).idArtista());
    }

    private CompraRegistradaRequest compra(String ciudad) {
        return new CompraRegistradaRequest(42L, 101L, 20L, "SHOW", ciudad,
                List.of(new ArtistaCompraRequest(7L, "Banda")));
    }
}
