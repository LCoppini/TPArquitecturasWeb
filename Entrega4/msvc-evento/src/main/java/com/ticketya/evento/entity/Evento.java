package com.ticketya.evento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_lugar", nullable = false)
    private Long idLugar;

    @Column(name = "id_organizador", nullable = false)
    private Long idOrganizador;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(nullable = false, length = 20)
    private String estado = com.ticketya.evento.entity.EstadosEvento.BORRADOR;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoParticipante> participantes = new ArrayList<>();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoSector> sectores = new ArrayList<>();

    public void reemplazarParticipantes(List<Participante> nuevosParticipantes) {
        participantes.forEach(relacion -> relacion.getParticipante().getEventos().remove(relacion));
        participantes.clear();
        nuevosParticipantes.forEach(this::agregarParticipante);
    }

    private void agregarParticipante(Participante participante) {
        EventoParticipante relacion = new EventoParticipante(this, participante);
        participantes.add(relacion);
        participante.getEventos().add(relacion);
    }
}
