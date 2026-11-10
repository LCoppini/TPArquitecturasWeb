package com.ticketya.evento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class EventoParticipanteId implements Serializable {

    @Column(name = "id_evento")
    private Long idEvento;

    @Column(name = "id_participante")
    private Long idParticipante;
}
