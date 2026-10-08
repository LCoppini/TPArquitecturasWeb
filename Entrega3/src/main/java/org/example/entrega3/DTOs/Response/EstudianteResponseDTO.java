package org.example.entrega3.DTOs.Response;

import java.time.LocalDate;
import java.util.List;

public record EstudianteResponseDTO(
        Long numLibreta,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String genero,
        Long dni,
        String ciudadResidencia,
        List<EstudianteCarreraResponseDTO> carreras
) {}
