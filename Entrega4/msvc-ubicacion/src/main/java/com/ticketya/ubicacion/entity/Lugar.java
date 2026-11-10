package com.ticketya.ubicacion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lugar")
@Getter
@Setter
@NoArgsConstructor
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 250)
    private String direccion;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<Sector> sectores = new ArrayList<>();

    public Lugar(String nombre, String direccion, String ciudad, Double latitud, Double longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public void agregarSector(Sector sector) {
        sector.setLugar(this);
        sectores.add(sector);
    }
}
