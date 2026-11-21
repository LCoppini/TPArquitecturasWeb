package com.ticketya.entrada.exception;

import com.ticketya.entrada.controller.AccesoController;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convierte los rechazos del escaneo de QR en respuestas con un código estable.
 * Está acotado a {@link AccesoController} para no alterar el manejo genérico de
 * los demás endpoints.
 */
@RestControllerAdvice(assignableTypes = AccesoController.class)
public class QrExceptionHandler implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @ExceptionHandler(QrRechazadoException.class)
    public ResponseEntity<Map<String, String>> manejarQrRechazado(QrRechazadoException exception) {
        return ResponseEntity.status(exception.getError().getHttpStatus())
                .body(Map.of(
                        "error", exception.getMessage(),
                        "code", exception.getError().name()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarRequestInvalido(MethodArgumentNotValidException exception) {
        String detalle = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(Map.of("error", detalle, "code", QrError.QR_REQUEST_INVALIDO.name()));
    }
}
