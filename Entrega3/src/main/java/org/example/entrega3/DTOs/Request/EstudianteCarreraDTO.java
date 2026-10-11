package org.example.entrega3.DTOs.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

public record EstudianteCarreraDTO(

        @NotNull(message = "No puede ser null el idCarrera")
        Long idCarrera,
        @NotNull(message = "No puede ser null el idCarrera")
        Long numLibreta,

        @NotNull(message = "No puede ser null")
        LocalDate fechaInscripcion,
        LocalDate fechaGraduacion

){
}
