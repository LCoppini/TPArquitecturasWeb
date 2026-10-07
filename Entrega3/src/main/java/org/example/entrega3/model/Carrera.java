package org.example.entrega3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter

@Entity

public class Carrera {
    @Id
    private Long idCarrera;

    @Column(nullable = false)
    private String nombreCarrera;

    @Column(nullable = false)
    private int duracion;

    @JsonIgnoreProperties("carrera")
    @OneToMany(mappedBy = "carrera")

    private List<EstudianteCarrera> estudiantes = new ArrayList<>();

    public Carrera() {
    }

    public Carrera(String nombreCarrera, Long idCarrera, int duracion) {
        this.nombreCarrera = nombreCarrera;
        this.idCarrera = idCarrera;
        this.duracion = duracion;
    }

}
