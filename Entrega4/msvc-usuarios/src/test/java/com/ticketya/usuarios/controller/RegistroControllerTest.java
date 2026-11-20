package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.request.RegistroRequest;
import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.service.RegistroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistroController.class)
class RegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroService registroService;

    @Test
    void registroPublicoDevuelve201ConRolCliente() throws Exception {
        when(registroService.registrar(any(RegistroRequest.class))).thenReturn(
                new UsuarioResponse(9L, 123L, "Ana", "Pérez", "Tandil", true,
                        Set.of(new RolResponse(1L, "CLIENTE"))));

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil",
                                 "email":"ana@ticketya.local","password":"secreto123",
                                 "roles":["ADMINISTRADOR"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0].tipo").value("CLIENTE"));
    }

    @Test
    void registroRequiereEmailYPassword() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":123,\"nombre\":\"Ana\",\"apellido\":\"Pérez\",\"ciudad\":\"Tandil\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void emailDuplicadoDevuelve409() throws Exception {
        when(registroService.registrar(any(RegistroRequest.class)))
                .thenThrow(new ConflictoException("El email ya está registrado"));

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil",
                                 "email":"ana@ticketya.local","password":"secreto123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
