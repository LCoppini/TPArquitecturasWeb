package DTO;

import java.time.LocalDate;

public class EstudianteDTO {

    private Long numLibreta;
    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private String genero;
    private Long dni;
    private String ciudadResidencia;

    public EstudianteDTO(Long numLibreta, String nombre, String apellido, LocalDate fechaNacimiento,
                         String genero, Long dni, String ciudadResidencia) {
        this.numLibreta = numLibreta;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
        this.dni = dni;
        this.ciudadResidencia = ciudadResidencia;
    }

    public Long getNumLibreta() { return numLibreta; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getGenero() { return genero; }
    public Long getDni() { return dni; }
    public String getCiudadResidencia() { return ciudadResidencia; }

    @Override
    public String toString() {
        return "Libreta " + numLibreta + ": " + nombre + " " + apellido +
                " (" + genero + ") - " + ciudadResidencia;
    }
}