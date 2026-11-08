package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.service.RolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolController.class)
class RolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RolService rolService;

    @Test
    void listaCatalogoDeRoles() throws Exception {
        when(rolService.listar()).thenReturn(List.of(
                new RolResponse(1L, "ADMINISTRADOR"),
                new RolResponse(2L, "CLIENTE"),
                new RolResponse(3L, "ORGANIZADOR"),
                new RolResponse(4L, "PERSONAL_ACCESO")
        ));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$[3].tipo").value("PERSONAL_ACCESO"));
    }
}
