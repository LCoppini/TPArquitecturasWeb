package com.ticketya.evento.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class EventoExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApiResponse> noEncontrado(
            RecursoNoEncontradoException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorApiResponse> conflicto(
            ReglaNegocioException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorApiResponse> prohibido(
            AccesoDenegadoException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<ErrorApiResponse> servicioExterno(
            ServicioExternoException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApiResponse> argumentoInvalido(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatearError)
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorApiResponse> solicitudInvalida(
            Exception exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    private String formatearError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorApiResponse> respuesta(
            HttpStatus status,
            String mensaje,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ErrorApiResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        ));
    }
}
