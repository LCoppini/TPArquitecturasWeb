package org.example.entrega3.DTOs.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.entrega3.model.Estudiante;

import java.time.LocalDate;
import java.util.List;


public record EstudianteDTO
        (
                @NotNull(message = "EL numero de libreta no puede ser null")
                Long num_liobreta,
                @NotNull(message = "EL nombre no puede ser null")
                String nombre,
                @NotNull(message = "El apellido no puede ser null")
                String apellido,
                @NotNull(message = "No puede ser nul la fecha de nacimiento")
                LocalDate fecha_nacimiento,
                @NotNull(message = "NO puede ser null el genero")
                String genero,
                @NotNull(message = "No puede ser null el Dni")
                @NotEmpty(message = "No puede ser vacio")
                Long Dni,
                @NotNull(message = "No puede ser null la ciudad")
                String ciudad_residencia,
                @NotNull(message = "La lista no puede tener valores nulos")
                @NotEmpty(message = "No puede ser vacia")
                List<EstudianteCarreraDTO> carreras
) {



}
