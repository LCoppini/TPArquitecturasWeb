package org.example.entrega3.DTOs.Response;

import java.time.LocalDate;

import org.example.entrega3.model.EstudianteCarrera;


public record EstudianteCarreraResponseDTO(
        Long numLibreta,
        String nombreEstudiante,
        String apellidoEstudiante,
        Long idCarrera,
        String nombreCarrera,
        LocalDate fechaInscripcion,
        LocalDate fechaGraduacion
) {
    public EstudianteCarreraResponseDTO(EstudianteCarrera ec) {
        this(
                ec.getEstudiante().getNum_libreta(),
                ec.getEstudiante().getNombre(),
                ec.getEstudiante().getApellido(),
                ec.getCarrera().getIdCarrera(),
                ec.getCarrera().getNombreCarrera(),
                ec.getFechaInscripcion(),
                ec.getFechaGraduacion()
        );
    }
}
