package org.example.entrega3.DTOs.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EstudianteCarreraDTO(
        @NotNull(message = "No puede ser nul el numero de libreta")
        @NotEmpty(message = "No puede ser vacio el num_libreta")
        Long numLibreta,
        @NotNull(message = "No puede ser null el idCarrera")
        @NotEmpty(message = "No puede ser vacio el idCarrera")
        Long idCarrera,
        @NotNull(message = "No puede ser null")
        LocalDate fechaInscripcion,
        @NotNull(message = "No puede ser null")
        LocalDate fechaGraduacion

){
}
