package Entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Estudiante {
    @Id
    @Column (name="num_libreta")
    //@GeneratedValue(strategy = GenerationType.AUTO) Preguntar en clase como implementarlo
    private Long num_libreta;

    @Column(length = 30, nullable = false)
    private String nombre;

    @Column(length = 30, nullable = false)
    private String apellido;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 30, nullable = false)
    private String genero;

    @Column(nullable = false)
    private Long dni;

    @Column(name = "ciudad_residencia", length = 30, nullable = false)
    private String ciudadResidencia;

    @OneToMany(mappedBy = "estudiante")
    private List<EstudianteCarrera> carreras = new ArrayList<>();

    public Estudiante() {
    }

    public Estudiante(Long num_libreta, String nombre, String apellido, LocalDate fechaNacimiento, String genero, Long dni, String ciudadResidencia) {
        this.num_libreta = num_libreta;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
        this.dni = dni;
        this.ciudadResidencia = ciudadResidencia;
    }

    public Long getNum_libreta() {
        return num_libreta;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public LocalDate getEdad() {

        return fechaNacimiento;
    }

    public Long getDni() {
        return dni;
    }

    public String getGenero() {
        return genero;
    }

    public String getCiudadResidencia() {
        return ciudadResidencia;
    }

    public void setNum_libreta(Long num_libreta) {
        this.num_libreta = num_libreta;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public void setDni(Long dni) {
        this.dni = dni;
    }

    public void setCiudadResidencia(String ciudadResidencia) {
        this.ciudadResidencia = ciudadResidencia;
    }

    public List<EstudianteCarrera> getCarreras() {
        return carreras;
    }

    public void setCarreras(List<EstudianteCarrera> carreras) {
        this.carreras = carreras;
    }
}
