package com.ticketya.notificaciones.client;

import com.ticketya.notificaciones.dto.response.UsuarioResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Datos del destinatario. contextId evita choques de beans con otros clients al mismo servicio. */
@FeignClient(name = "msvc-usuarios", contextId = "usuarioClient", path = "/api/usuarios")
public interface UsuarioClient {

    @GetMapping("/{id}")
    UsuarioResponse obtenerPorId(@PathVariable("id") Long id);
}
