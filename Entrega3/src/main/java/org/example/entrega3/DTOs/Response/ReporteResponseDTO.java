package org.example.entrega3.DTOs.Response;


public record ReporteResponseDTO(
        Long idCarrera,
        String nombreCarrera,
        Integer anio,
        Long cantidadDeinscriptos,
        Long cantidadDeEgresados
) {}

