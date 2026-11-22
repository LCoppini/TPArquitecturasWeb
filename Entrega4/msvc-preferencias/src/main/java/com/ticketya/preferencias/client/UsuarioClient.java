package com.ticketya.preferencias.client;

import com.ticketya.preferencias.client.dto.UsuarioItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "msvc-usuarios", path = "/api/usuarios")
public interface UsuarioClient {

    @GetMapping("/{id}")
    UsuarioItem obtenerPorId(@PathVariable("id") Long id);
}
