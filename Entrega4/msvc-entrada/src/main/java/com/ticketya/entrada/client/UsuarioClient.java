package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.response.UsuarioResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Consulta Usuarios por su API, sin replicar su información en msvc-entrada.
 * El nombre del servicio permite que Feign lo resuelva mediante Eureka.
 */
@FeignClient(name = "msvc-usuarios", path = "/api/usuarios")
public interface UsuarioClient {

    @GetMapping("/{id}")
    UsuarioResponse obtenerPorId(@PathVariable("id") Long id);
}
