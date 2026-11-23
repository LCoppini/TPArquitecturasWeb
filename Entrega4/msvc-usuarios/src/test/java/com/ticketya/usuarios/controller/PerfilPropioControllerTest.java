package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.exception.RecursoNoEncontradoException;
import com.ticketya.usuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/usuarios/me: el usuario autenticado consulta su propio perfil (identidad por X-User-Id). */
@WebMvcTest(UsuarioController.class)
class PerfilPropioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void miPerfilDevuelveElUsuarioDelHeaderConSuEmail() throws Exception {
        when(usuarioService.buscarPorId(9L)).thenReturn(new UsuarioResponse(
                9L, 30111222L, "Ana", "Pérez", "Tandil", true,
                Set.of(new RolResponse(1L, "CLIENTE")), "ana@ticketya.dev"));

        mockMvc.perform(get("/api/usuarios/me").header("X-User-Id", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(9))
                .andExpect(jsonPath("$.email").value("ana@ticketya.dev"))
                .andExpect(jsonPath("$.roles[0].tipo").value("CLIENTE"));
    }

    @Test
    void miPerfilSinHeaderDeIdentidadEsSolicitudInvalida() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void miPerfilDeUsuarioInactivoDevuelve404() throws Exception {
        when(usuarioService.buscarPorId(9L)).thenThrow(new RecursoNoEncontradoException("No encontrado"));

        mockMvc.perform(get("/api/usuarios/me").header("X-User-Id", "9"))
                .andExpect(status().isNotFound());
    }
}
