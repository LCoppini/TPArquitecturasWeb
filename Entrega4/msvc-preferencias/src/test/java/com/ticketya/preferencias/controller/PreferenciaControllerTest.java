package com.ticketya.preferencias.controller;

import com.ticketya.preferencias.dto.response.ArtistaPreferidoResponse;
import com.ticketya.preferencias.dto.response.PreferenciaClienteResponse;
import com.ticketya.preferencias.service.PreferenciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PreferenciaControllerTest {

    private PreferenciaService service;
    private MockMvc clienteMvc;
    private MockMvc adminMvc;

    @BeforeEach
    void setUp() {
        service = mock(PreferenciaService.class);
        clienteMvc = MockMvcBuilders.standaloneSetup(new PreferenciaController(service)).build();
        adminMvc = MockMvcBuilders.standaloneSetup(new PreferenciaAdminController(service)).build();
    }

    @Test
    void clienteConsultaSuPropiaPreferencia() throws Exception {
        when(service.obtener(42L)).thenReturn(respuesta());

        clienteMvc.perform(get("/api/preferencias/clientes/42").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(42L))
                .andExpect(jsonPath("$.ciudad").value("Tandil"))
                .andExpect(jsonPath("$.tiposEvento.SHOW").value(2))
                .andExpect(jsonPath("$.artistas[0].idArtista").value(7L))
                .andExpect(jsonPath("$.artistas[0].cantidad").value(2))
                .andExpect(jsonPath("$.comprasProcesadas").doesNotExist())
                .andExpect(jsonPath("$.eventosComprados").doesNotExist())
                .andExpect(jsonPath("$.actualizadoEn").doesNotExist());
    }

    @Test
    void clienteNoPuedeConsultarLaPreferenciaDeOtro() throws Exception {
        clienteMvc.perform(get("/api/preferencias/clientes/42").header("X-User-Id", "99"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void administradorConsultaCualquierPreferenciaSinCompararElHeader() throws Exception {
        when(service.obtener(42L)).thenReturn(respuesta());

        adminMvc.perform(get("/api/admin/preferencias/clientes/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(42L));

        verify(service).obtener(42L);
    }

    @Test
    void administradorPuedeEnviarOtroHeaderSinQueAfecteLaConsulta() throws Exception {
        when(service.obtener(anyLong())).thenReturn(respuesta());

        adminMvc.perform(get("/api/admin/preferencias/clientes/42").header("X-User-Id", "99"))
                .andExpect(status().isOk());

        verify(service).obtener(42L);
    }

    private PreferenciaClienteResponse respuesta() {
        return new PreferenciaClienteResponse(42L, "Tandil", Map.of("SHOW", 2),
                List.of(new ArtistaPreferidoResponse(7L, "Banda", 2)));
    }
}
