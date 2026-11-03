package org.example.entrega3.controller.estudiante;

import org.example.entrega3.DTOs.ErrorDTO;
import org.example.entrega3.Exception.EstudianteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "org.example.entrega3.controller.estudiante" )
public class EstudianteExceptionHandler {
    @ExceptionHandler( exception = EstudianteException.class )
    public ResponseEntity<ErrorDTO> handleEstudiante(EstudianteException ex) {
        ErrorDTO error = new ErrorDTO(ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }


}
