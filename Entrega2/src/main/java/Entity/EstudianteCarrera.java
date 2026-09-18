package Entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class EstudianteCarrera {


    @EmbeddedId
    private EstudianteCPK estudianteCPK;

    @ManyToOne
    private Estudiante estudiante;

    @ManyToOne
    private Carrera carrera;

    @Column
    private LocalDate fechaDeinscripcion;

    @Column
    private LocalDate fechaGraduacion;

    public EstudianteCarrera(Estudiante estudiante, Carrera carrera) {
    }

    public EstudianteCarrera(EstudianteCPK estudianteCPK,LocalDate fechaDeinscripcion,Carrera carrera,Estudiante estudiante) {
        this.estudianteCPK = estudianteCPK;
        this.fechaGraduacion = fechaGraduacion;
        this.fechaDeinscripcion = fechaDeinscripcion;
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
}
