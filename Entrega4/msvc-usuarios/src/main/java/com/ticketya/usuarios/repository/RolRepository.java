package com.ticketya.usuarios.repository;

import com.ticketya.usuarios.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface RolRepository extends JpaRepository<Rol, Long> {

    List<Rol> findAllByOrderByTipoAsc();

    List<Rol> findAllByTipoIn(Set<String> tipos);

    @Modifying
    @Query(value = "INSERT INTO rol (tipo) VALUES (:tipo) ON CONFLICT (tipo) DO NOTHING", nativeQuery = true)
    int insertarSiNoExiste(@Param("tipo") String tipo);
}
