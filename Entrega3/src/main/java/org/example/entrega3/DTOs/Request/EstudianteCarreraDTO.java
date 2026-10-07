package org.example.entrega3.DTOs.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;


public class EstudianteCarreraDTO(
        @NotNull(message = "No puede ser nul el numero de libreta")
        @NotEmpty(message = "No puede ser vacio el num_libreta")
        Long num_libreta,
        @NotNull(message = "No puede ser null el idCarrera")
        @NotEmpty(message = "No puede ser vacio el idCarrera")
        Long idCarrera,
        @NotNull(message = "No puede ser null")
        LocalDate fecha_incripcion,
        LocalDate fecha_graduacion
){
}
