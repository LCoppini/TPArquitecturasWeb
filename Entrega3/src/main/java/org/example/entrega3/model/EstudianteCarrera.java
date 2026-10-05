package org.example.entrega3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
@Getter
@Setter
@Entity
public class EstudianteCarrera {

    @EmbeddedId
    private EstudianteCPK estudianteCPK;

    @ManyToOne
    @MapsId("idEstudiante") // le dice a hibernate que el campo embeddebId y estudiante son lo mismo y no dos cosas distintas
    @JoinColumn(name = "num_libreta")
    private Estudiante estudiante;

    @ManyToOne
    @MapsId("idCarrera")
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Column(name = "fecha_graduacion", nullable = true)
    private LocalDate fechaGraduacion;

    public EstudianteCarrera(Estudiante estudiante, Carrera carrera,  LocalDate fechaInscripcion) {
        this.carrera = carrera;
        this.estudiante = estudiante;
        this.fechaInscripcion = fechaInscripcion;
        this.estudianteCPK = new EstudianteCPK(estudiante.getNum_libreta(),carrera.getIdCarrera());
    }


    public EstudianteCarrera() {

    }

    public boolean estaGraduado() {
        return fechaGraduacion != null;
    }

//    int anio = 2022;
//
//    // Crea la fecha 1 de enero de 2022
//    LocalDate fecha = LocalDate.of(anio, 1, 1);

    public int getAntiguedadAnios() {
        LocalDate hasta;
        if (fechaGraduacion != null)
            hasta = fechaGraduacion;
        else
            hasta = LocalDate.now();

        return Period.between(fechaInscripcion, hasta).getYears();
    }
}
