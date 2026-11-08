package com.ticketya.usuarios.exception;

import com.ticketya.usuarios.dto.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException exception) {
        Map<String, String> detalles = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(HttpMessageNotReadableException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido", Map.of());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudInvalida(SolicitudInvalidaException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException exception) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> manejarConflicto(ConflictoException exception) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException exception) {
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con una restricción de datos", Map.of());
    }

    private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje, Map<String, String> detalles) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), mensaje, detalles));
    }
}
