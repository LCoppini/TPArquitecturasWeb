package com.ticketya.evento.repository;

import com.ticketya.evento.entity.EventoParticipante;
import com.ticketya.evento.entity.EventoParticipanteId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, EventoParticipanteId> {

    boolean existsByIdIdParticipante(Long idParticipante);
}
