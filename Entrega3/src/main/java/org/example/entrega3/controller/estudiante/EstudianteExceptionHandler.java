package org.example.entrega3.controller.estudiante;

import org.example.entrega3.DTOs.ErrorDTO;
import org.example.entrega3.Exception.EstudianteException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.DataTruncation;
import java.time.LocalDate;

@RestControllerAdvice(basePackages = "org.example.entrega3.controller.estudiante" )
public class EstudianteExceptionHandler {
    @ExceptionHandler( exception = EstudianteException.class )
    public ErrorDTO notFoundException(EstudianteException ex ) {
        return new ErrorDTO( ex.getMessage(), LocalDate.now() );
    }

    @ExceptionHandler( exception = DataTruncation.class )
    public ErrorDTO notFoundException( DataTruncation ex ) {
        return new ErrorDTO( ex.getMessage(), LocalDate.now() );
    }
}
