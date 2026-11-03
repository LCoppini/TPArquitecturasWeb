package org.example.entrega3.controller.carrera;

import org.example.entrega3.DTOs.ErrorDTO;
import org.example.entrega3.Exception.CarreraException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestControllerAdvice(basePackages = "org.example.entrega3.controller.carrera")
public class CarreraExceptionHandler {
    @ExceptionHandler( exception = CarreraException.class )
    public ResponseEntity<ErrorDTO> handleCarrera(CarreraException ex) {
        ErrorDTO error = new ErrorDTO(ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}