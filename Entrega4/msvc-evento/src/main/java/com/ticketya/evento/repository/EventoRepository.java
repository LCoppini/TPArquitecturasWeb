package com.ticketya.evento.repository;

import com.ticketya.evento.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Long> {
}
