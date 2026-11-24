package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.exception.RecursoNoEncontradoException;
import com.ticketya.usuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PUT /api/usuarios/me/ciudad: el usuario autenticado cambia solo su ciudad (identidad por X-User-Id). */
@WebMvcTest(UsuarioController.class)
class CiudadPropiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void cambiaLaCiudadDelUsuarioDelHeader() throws Exception {
        when(usuarioService.actualizarCiudad(9L, "Rosario")).thenReturn(new UsuarioResponse(
                9L, 30111222L, "Ana", "Pérez", "Rosario", true,
                Set.of(new RolResponse(1L, "CLIENTE")), "ana@ticketya.dev"));

        mockMvc.perform(put("/api/usuarios/me/ciudad").header("X-User-Id", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ciudad\":\"Rosario\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciudad").value("Rosario"));
    }

    @Test
    void ciudadVaciaDevuelve400() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/ciudad").header("X-User-Id", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ciudad\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(usuarioService, never()).actualizarCiudad(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioInactivoDevuelve404() throws Exception {
        when(usuarioService.actualizarCiudad(9L, "Rosario"))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el usuario activo con id 9"));

        mockMvc.perform(put("/api/usuarios/me/ciudad").header("X-User-Id", "9")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ciudad\":\"Rosario\"}"))
                .andExpect(status().isNotFound());
    }
}
