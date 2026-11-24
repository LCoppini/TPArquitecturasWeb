package com.ticketya.entrada.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ticketya.entrada.dto.response.EntradaResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La entrada expone a qué Compra pertenece y su estado de devolución, para "Mis entradas". */
class EntradaRespuestaCompraTest {

    @Test
    void laRespuestaIncluyeIdCompraYEstadoDeDevolucion() {
        EntradaResponse respuesta = new EntradaResponse(1L, 20L, 10L, 30L, 1, 2, BigDecimal.TEN, "qr", "VALIDA",
                null, 55L, "PENDIENTE", null);

        JsonNode json = new ObjectMapper().valueToTree(respuesta);

        assertEquals(55L, json.get("idCompra").asLong());
        assertEquals("PENDIENTE", json.get("estadoDevolucion").asText());
        assertTrue(json.get("motivoInvalidacion").isNull());
    }

    @Test
    void elConstructorPrevioDejaLosCamposNuevosEnNulo() {
        EntradaResponse respuesta = new EntradaResponse(1L, 20L, 10L, 30L, null, null, BigDecimal.TEN, "qr", "VALIDA",
                null);

        assertEquals(null, respuesta.getIdCompra());
    }
}
