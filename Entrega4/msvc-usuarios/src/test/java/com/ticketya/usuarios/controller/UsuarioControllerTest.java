package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.dto.request.ActualizarUsuarioRequest;
import com.ticketya.usuarios.dto.request.CrearUsuarioRequest;
import com.ticketya.usuarios.exception.ConflictoException;
import com.ticketya.usuarios.exception.RecursoNoEncontradoException;
import com.ticketya.usuarios.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void consultaPorIdExponeIdUsuarioCompatibleConFeign() throws Exception {
        when(usuarioService.buscarPorId(4L)).thenReturn(new UsuarioResponse(
                4L, 123L, "Ana", "Pérez", "Tandil", true,
                Set.of(new RolResponse(1L, "CLIENTE"))
        ));

        mockMvc.perform(get("/api/usuarios/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(4))
                .andExpect(jsonPath("$.roles[0].tipo").value("CLIENTE"));
    }

    @Test
    void listarDevuelveUsuariosActivos() throws Exception {
        when(usuarioService.listar()).thenReturn(java.util.List.of(
                new UsuarioResponse(4L, 123L, "Ana", "Pérez", "Tandil", true, Set.of())
        ));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(4));
    }

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        when(usuarioService.buscarPorId(404L)).thenThrow(new RecursoNoEncontradoException("No encontrado"));

        mockMvc.perform(get("/api/usuarios/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void altaConCamposObligatoriosAusentesDevuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.dni").exists());
    }

    @Test
    void altaValidaDevuelve201() throws Exception {
        when(usuarioService.crear(any(CrearUsuarioRequest.class))).thenReturn(
                new UsuarioResponse(9L, 123L, "Ana", "Pérez", "Tandil", true,
                        Set.of(new RolResponse(1L, "CLIENTE"))));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil","roles":["CLIENTE"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(9));
    }

    @Test
    void dniDuplicadoDevuelve409() throws Exception {
        when(usuarioService.crear(any(CrearUsuarioRequest.class)))
                .thenThrow(new ConflictoException("El DNI ya está registrado"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil","roles":["CLIENTE"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void conflictoDeIntegridadDeBaseDeDatosDevuelve409() throws Exception {
        when(usuarioService.crear(any(CrearUsuarioRequest.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil","roles":["CLIENTE"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void modificacionValidaDevuelve200() throws Exception {
        when(usuarioService.actualizar(org.mockito.ArgumentMatchers.eq(9L), any(ActualizarUsuarioRequest.class)))
                .thenReturn(new UsuarioResponse(9L, 123L, "Ana", "Pérez", "Tandil", true, Set.of()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/usuarios/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ciudad").value("Tandil"));
    }

    @Test
    void bajaDevuelve204() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/usuarios/9"))
                .andExpect(status().isNoContent());
        verify(usuarioService).eliminar(9L);
    }

    @Test
    void reemplazoDeRolesDevuelveUsuarioActualizado() throws Exception {
        when(usuarioService.reemplazarRoles(9L, Set.of())).thenReturn(
                new UsuarioResponse(9L, 123L, "Ana", "Pérez", "Tandil", true, Set.of()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/usuarios/9/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isEmpty());
    }
}
