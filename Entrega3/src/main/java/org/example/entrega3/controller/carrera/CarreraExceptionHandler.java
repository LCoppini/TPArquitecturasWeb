package org.example.entrega3.controller.carrera;

import org.example.entrega3.DTOs.ErrorDTO;
import org.example.entrega3.Exception.CarreraException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;

@RestControllerAdvice(basePackages = "org.example.entrega3.controller.carrera")
public class CarreraExceptionHandler {
    @ExceptionHandler( exception = CarreraException.class )
    public ErrorDTO notFoundException(CarreraException ex ) {
        return new ErrorDTO( ex.getMessage(), LocalDate.now() );
    }
}