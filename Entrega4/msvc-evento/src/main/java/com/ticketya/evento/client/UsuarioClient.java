package com.ticketya.evento.client;

import com.ticketya.evento.dto.response.UsuarioResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "msvc-usuarios", path = "/api/usuarios")
public interface UsuarioClient {

    @GetMapping("/{id}")
    UsuarioResponse obtenerPorId(@PathVariable("id") Long id);
}
