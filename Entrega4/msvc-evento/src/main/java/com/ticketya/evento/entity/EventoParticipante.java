package com.ticketya.evento.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_participante")
@Getter
@Setter
@NoArgsConstructor
public class EventoParticipante {

    @EmbeddedId
    private EventoParticipanteId id = new EventoParticipanteId();

    @ManyToOne(optional = false)
    @MapsId("idEvento")
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @ManyToOne(optional = false)
    @MapsId("idParticipante")
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    public EventoParticipante(Evento evento, Participante participante) {
        this.evento = evento;
        this.participante = participante;
    }
}
