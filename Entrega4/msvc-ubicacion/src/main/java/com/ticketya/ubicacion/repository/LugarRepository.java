package com.ticketya.ubicacion.repository;

import com.ticketya.ubicacion.entity.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LugarRepository extends JpaRepository<Lugar, Long> {

    List<Lugar> findByCiudadIgnoreCaseOrderByIdAsc(String ciudad);

    List<Lugar> findAllByOrderByIdAsc();
}
