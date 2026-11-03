package org.example.entrega3.DTOs.Response;

public record CarreraInscriptosResponseDTO(
        Long idCarrera,
        String nombreCarrera,
        int duracion,
        Long cantidadInscriptos
) {}
