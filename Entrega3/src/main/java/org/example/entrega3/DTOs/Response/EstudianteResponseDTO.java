package org.example.entrega3.DTOs.Response;

import lombok.Getter;
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
    private LocalDate fechaNacimiento;
    private String genero;
    private Long dni;
    private String ciudadResidencia;
    private List<EstudianteCarreraResponseDTO> carreras;

    public EstudianteResponseDTO() {}

    public EstudianteResponseDTO(Estudiante e) {
        this.numLibreta = e.getNum_libreta();
        this.nombre = e.getNombre();
        this.apellido = e.getApellido();
        this.fechaNacimiento = e.getFechaNacimiento();
        this.genero = e.getGenero();
        this.dni = e.getDni();
        this.ciudadResidencia = e.getCiudadResidencia();
        this.carreras = e.getCarreras().stream()
                .map(EstudianteCarreraResponseDTO::new)
                .toList();
    }


}
