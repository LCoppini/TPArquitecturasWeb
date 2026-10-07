package org.example.entrega3.DTOs.Response;

import org.example.entrega3.model.Carrera;
import org.example.entrega3.model.Estudiante;

public class EstudianteResponseDTO {
    private String nombre;
    private String apellido;
    private String genero;
    private String ciudad;


    public EstudianteResponseDTO(Estudiante e) {
        this.nombre = e.getNombre();
        this.apellido = e.getApellido();
        this.genero = e.getGenero();
        this.ciudad = e.getCiudadResidencia();
    }
}
