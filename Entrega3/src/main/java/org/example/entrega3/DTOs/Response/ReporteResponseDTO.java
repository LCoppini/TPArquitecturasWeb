package org.example.entrega3.DTOs.Response;


public record ReporteResponseDTO(
        String nombreCarrera,
        Integer anio,
        Long cantidadDeinscriptos,
        Long cantidadDeEgresados
) {}

