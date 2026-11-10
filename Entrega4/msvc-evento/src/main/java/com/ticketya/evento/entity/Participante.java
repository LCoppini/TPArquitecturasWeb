package com.ticketya.evento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participante")
@Getter
@Setter
@NoArgsConstructor
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String tipo;

    @OneToMany(mappedBy = "participante")
    private List<EventoParticipante> eventos = new ArrayList<>();

    public Participante(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }
}
