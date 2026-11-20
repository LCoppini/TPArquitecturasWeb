package com.ticketya.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.gateway.exception.GatewayErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Escribe 401 y 403 con el mismo contrato JSON que el resto de errores del gateway. */
@Component
public class GatewaySecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public GatewaySecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response,
                      HttpServletRequest request,
                      HttpStatus status,
                      String message) throws IOException {

        GatewayErrorResponse body = new GatewayErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
