package com.ticketya.ubicacion.repository;

import com.ticketya.ubicacion.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    List<Sector> findByLugarIdOrderByIdAsc(Long idLugar);

    boolean existsByLugarIdAndNombreIgnoreCase(Long idLugar, String nombre);
}
