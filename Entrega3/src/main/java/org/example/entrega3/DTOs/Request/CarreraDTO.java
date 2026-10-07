package org.example.entrega3.DTOs.Request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CarreraDTO(
        @NotNull(message = "El nombre no pudo ser vacio")
        String nombreCarrera,
        @NotNull(message =  "El id no puede ser null")
        Long id,
        @NotNull(message = "la duracion no puede ser nula")
        int duracion,
        @NotNull(message = "La lista de estudiantes no puede ser nulo")
        @NotEmpty(message = "La lista no puede ser vacia")
        List<EstudianteCarreraDTO> estudiantes
) {


}
