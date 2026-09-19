package Entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.Period;

@Entity
public class EstudianteCarrera {

    @EmbeddedId
    private EstudianteCPK estudianteCPK;

    @ManyToOne
    @MapsId("idEstudiante") // le dice a hibernate que el campo embeddebId y estudiante son lo mismo y no dos cosas distintas
    @JoinColumn("num_libreta")
    private Estudiante estudiante;

    @ManyToOne
    @MapsId("idCarrera")
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Column(name = "fecha_graduacion", nullable = true)
    private LocalDate fechaGraduacion;

    public EstudianteCarrera(Estudiante estudiante, Carrera carrera) {
    }

    public EstudianteCarrera(EstudianteCPK estudianteCPK,LocalDate fechaDeinscripcion,Carrera carrera,Estudiante estudiante) {
        this.estudianteCPK = estudianteCPK;
        this.fechaInscripcion = fechaDeinscripcion;
        this.carrera = carrera;
        this.estudiante = estudiante;
    }

    public EstudianteCarrera() {

    }


    public EstudianteCPK getEstudianteCPK() {
        return estudianteCPK;
    }

    public void setEstudianteCPK(EstudianteCPK estudianteCPK) {
        this.estudianteCPK = estudianteCPK;
    }

    public Carrera getCarrera() {
        return carrera;
    }

    public void setCarrera(Carrera carrera) {
        this.carrera = carrera;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public LocalDate getFechaDeinscripcion() {
        return fechaDeinscripcion;
    }

    public void setFechaDeinscripcion(LocalDate fechaDeinscripcion) {
        this.fechaDeinscripcion = fechaDeinscripcion;
    }

    public LocalDate getFechaGraduacion() {
        return fechaGraduacion;
    }

    public void setFechaGraduacion(LocalDate fechaGraduacion) {
        this.fechaGraduacion = fechaGraduacion;
    }


    public boolean estaGraduado() {
        return fechaGraduacion != null;
    }

    public int getAntiguedadAnios() {
        LocalDate hasta;
        if (fechaGraduacion != null)
            hasta = fechaGraduacion;
        else
            hasta = LocalDate.now();

        return Period.between(fechaInscripcion, hasta).getYears();
    }
}
