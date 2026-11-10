package com.ticketya.evento.repository;

import com.ticketya.evento.entity.EventoSector;
import com.ticketya.evento.entity.EventoSectorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoSectorRepository extends JpaRepository<EventoSector, EventoSectorId> {

    Optional<EventoSector> findByIdIdEventoAndIdIdSector(Long idEvento, Long idSector);
}
