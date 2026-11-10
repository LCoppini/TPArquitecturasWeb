package com.ticketya.evento.client;

import com.ticketya.evento.dto.response.LugarResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "msvc-ubicacion", contextId = "lugarClient", path = "/api/lugares")
public interface LugarClient {

    @GetMapping("/{id}")
    LugarResponse obtenerPorId(@PathVariable("id") Long id);
}
