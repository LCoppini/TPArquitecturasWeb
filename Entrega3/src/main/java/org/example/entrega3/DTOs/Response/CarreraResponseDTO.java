package org.example.entrega3.DTOs.Response;

import java.util.List;

public record CarreraResponseDTO(
        Long id,
        String nombreCarrera,
        int duracion,
        List<EstudianteCarreraResponseDTO> estudiantes
) {}
