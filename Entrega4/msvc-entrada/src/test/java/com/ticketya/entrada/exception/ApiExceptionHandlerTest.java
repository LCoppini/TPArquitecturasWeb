package com.ticketya.entrada.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void argumentosInvalidosRespondenBadRequest() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                handler.manejarArgumentoInvalido(new IllegalArgumentException("valor inválido")).getStatusCode()
        );
    }

    @Test
    void recursosAusentesRespondenNotFound() {
        assertEquals(
                HttpStatus.NOT_FOUND,
                handler.manejarNoEncontrado(new EntityNotFoundException("no encontrado")).getStatusCode()
        );
    }
}
