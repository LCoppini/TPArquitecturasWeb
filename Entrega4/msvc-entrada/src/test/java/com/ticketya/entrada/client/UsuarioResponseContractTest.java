package com.ticketya.entrada.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsuarioResponseContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void deserializaLaRespuestaDeMsvcUsuariosPorIdUsuario() throws Exception {
        UsuarioResponse response = objectMapper.readValue("""
                {"idUsuario":42,"dni":123,"nombre":"Ana","apellido":"Pérez","ciudad":"Tandil",
                 "activo":true,"roles":[{"idRol":1,"tipo":"CLIENTE"}]}
                """, UsuarioResponse.class);

        assertEquals(42L, response.idUsuario());
    }
}
