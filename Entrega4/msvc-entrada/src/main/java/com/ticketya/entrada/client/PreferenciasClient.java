package com.ticketya.entrada.client;

import com.ticketya.entrada.dto.request.CompraRegistradaRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Envío interno del outbox; no está expuesto en el gateway. */
@FeignClient(name = "msvc-preferencias", path = "/internal/preferencias")
public interface PreferenciasClient {

    @PostMapping("/compras")
    void registrar(@RequestBody CompraRegistradaRequest compra);
}
