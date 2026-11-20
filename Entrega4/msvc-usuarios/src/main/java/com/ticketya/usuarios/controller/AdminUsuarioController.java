package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.request.CrearUsuarioAdministrativoRequest;
import com.ticketya.usuarios.dto.response.ErrorResponse;
import com.ticketya.usuarios.dto.response.UsuarioResponse;
import com.ticketya.usuarios.service.AdminUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Alta de usuarios privilegiados; el gateway autoriza este prefijo a ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "AdminUsuarios", description = "Alta de usuarios con roles privilegiados")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @PostMapping
    @Operation(summary = "Crear un usuario con roles explicitos")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol desconocido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email o DNI ya registrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioAdministrativoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUsuarioService.crear(request));
    }
}
