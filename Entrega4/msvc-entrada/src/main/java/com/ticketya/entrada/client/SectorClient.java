package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.response.SectorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Obtiene capacidad y configuración de asientos desde msvc-ubicacion.
 */
@FeignClient(name = "msvc-ubicacion", path = "/api/sectores")
public interface SectorClient {

    @GetMapping("/{id}")
    SectorResponse obtenerPorId(@PathVariable("id") Long id);
}
