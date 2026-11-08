package com.ticketya.usuarios.repository;

import com.ticketya.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "roles")
    List<Usuario> findAllByActivoTrueOrderByIdUsuarioAsc();

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByIdUsuarioAndActivoTrue(Long idUsuario);

    boolean existsByDni(Long dni);

    boolean existsByDniAndIdUsuarioNot(Long dni, Long idUsuario);
}
