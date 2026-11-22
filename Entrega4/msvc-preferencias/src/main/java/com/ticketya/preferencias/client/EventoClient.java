package com.ticketya.preferencias.client;

import com.ticketya.preferencias.client.dto.EventoBusquedaResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

/**
 * Consulta al catálogo público de msvc-evento. El filtro `participante` acepta el id
 * del participante como texto (ver EventoServiceImpl.coincideParticipante).
 */
@FeignClient(name = "msvc-evento", path = "/api/eventos")
public interface EventoClient {

    @GetMapping
    EventoBusquedaResponse buscar(
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "participante", required = false) String participante,
            @RequestParam(name = "ciudad", required = false) String ciudad,
            @RequestParam(name = "desde", required = false) Instant desde,
            @RequestParam(name = "orden", defaultValue = "fecha") String orden,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamano", defaultValue = "50") int tamano);
}
