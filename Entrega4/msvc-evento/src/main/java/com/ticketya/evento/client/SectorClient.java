package com.ticketya.evento.client;

import com.ticketya.evento.dto.response.SectorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "msvc-ubicacion", path = "/api/sectores")
public interface SectorClient {

    @GetMapping("/{id}")
    SectorResponse obtenerPorId(@PathVariable("id") Long id);
}
