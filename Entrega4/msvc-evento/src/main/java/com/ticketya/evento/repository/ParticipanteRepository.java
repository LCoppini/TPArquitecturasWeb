package com.ticketya.evento.repository;

import com.ticketya.evento.entity.Participante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    Optional<Participante> findByNombreIgnoreCaseAndTipoIgnoreCase(String nombre, String tipo);
}
