package org.example.entrega3.DTOs;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.model.Estudiante;

@RequiredArgsConstructor
public class EstudianteDTO {
    private String nombre;
    private String apellido;
    private String genero;

    public EstudianteDTO(String nombre, String apellido, String genero) {
    }

    public EstudianteDTO(Estudiante estudiante) {
        this.nombre = estudiante.getNombre();
        this.apellido = estudiante.getApellido();
        this.genero = estudiante.getGenero();
    }
}
