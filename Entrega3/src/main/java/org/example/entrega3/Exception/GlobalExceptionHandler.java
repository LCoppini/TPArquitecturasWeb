package org.example.entrega3.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice//proba si devuelve un 404
public class GlobalExceptionHandler {

    @ExceptionHandler(EstudianteException.class)
    public ResponseEntity<String> handleEstudianteException(EstudianteException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
