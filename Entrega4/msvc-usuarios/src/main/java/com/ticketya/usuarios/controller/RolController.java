package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.response.RolResponse;
import com.ticketya.usuarios.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = "Consulta de los roles disponibles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @Operation(summary = "Listar los roles disponibles")
    @ApiResponse(responseCode = "200", description = "Catálogo de roles")
    public List<RolResponse> listar() {
        return rolService.listar();
    }
}
