package org.example.entrega3.DTOs.Response;

import org.example.entrega3.model.Carrera;

import java.util.List;

public record CarreraResponseDTO(
        Long id,
        String nombreCarrera,
        int duracion,
        List<EstudianteCarreraResponseDTO> estudiantes
) {
    public CarreraResponseDTO(Carrera c) {
        this(
                c.getIdCarrera(),
                c.getNombreCarrera(),
                c.getDuracion(),
                c.getEstudiantes().stream()
                        .map(EstudianteCarreraResponseDTO::new)
                        .toList()
        );
    }
}
