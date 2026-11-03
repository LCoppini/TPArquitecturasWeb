package org.example.entrega3.DTOs.Response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.entrega3.model.Estudiante;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter

public class EstudianteResponseDTO {
    private Long numLibreta;
    private String nombre;
    private String apellido;
    private String genero;
    private String ciudadResidencia;


    public EstudianteResponseDTO() {}

    public EstudianteResponseDTO(Estudiante e) {
        this.numLibreta = e.getNum_libreta();
        this.nombre = e.getNombre();
        this.apellido = e.getApellido();
        this.genero = e.getGenero();
        this.ciudadResidencia = e.getCiudadResidencia();
    }


}
