package org.example.entrega3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Setter
@Getter
@NoArgsConstructor
public class Estudiante {
    @Id
    @Column(name="num_libreta")
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

    @JsonIgnoreProperties("estudiante")
    @OneToMany(mappedBy = "estudiante")
    private List<EstudianteCarrera> carreras = new ArrayList<>();

    public Estudiante(Long num_libreta, String nombre, String apellido,
                      LocalDate fechaNacimiento, String genero, Long dni,
                      String ciudadResidencia) {
        this.num_libreta = num_libreta;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
        this.dni = dni;
        this.ciudadResidencia = ciudadResidencia;
    }




}
