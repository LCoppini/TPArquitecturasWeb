package org.example.entrega3.DTOs.Response;

import java.time.LocalDate;

public record EstudianteCarreraResponseDTO(
        Long numLibreta,
        String nombreEstudiante,
        String apellidoEstudiante,
        Long idCarrera,
        String nombreCarrera,
        LocalDate fechaInscripcion,
        LocalDate fechaGraduacion
) {}
