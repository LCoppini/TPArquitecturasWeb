package com.ticketya.notificaciones.client;

import com.ticketya.notificaciones.dto.response.ClientesDeArtistaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Clientes que siguen a un artista. contextId evita choques de beans con otros clients. */
@FeignClient(name = "msvc-preferencias", contextId = "preferenciasClient", path = "/internal/preferencias")
public interface PreferenciasClient {

    @GetMapping("/artistas/{idArtista}/clientes")
    ClientesDeArtistaResponse clientesDelArtista(@PathVariable("idArtista") Long idArtista,
                                                 @RequestParam("ciudad") String ciudad);
}
