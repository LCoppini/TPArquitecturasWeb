package com.ticketya.evento.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EntradaClientContratoTest {

    @Test
    void cancelacionApuntaALaRutaAdministrativaDeEntrada() throws Exception {
        Method metodo = EntradaClient.class.getMethod("cancelarEntradas", Long.class, Long.class);
        PostMapping mapping = metodo.getAnnotation(PostMapping.class);

        assertNotNull(mapping);
        assertEquals("/api/admin/entradas/eventos/{idEvento}/cancelacion", mapping.value()[0]);

        RequestHeader header = metodo.getParameters()[1].getAnnotation(RequestHeader.class);
        assertNotNull(header);
        assertEquals("X-User-Id", header.value());
    }
}
