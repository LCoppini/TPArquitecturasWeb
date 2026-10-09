package org.example.entrega3.repository;

import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.model.Estudiante;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    //d-recuperar un estudiante en base a su num_libreta
    @Query("SELECT e FROM Estudiante e WHERE e.num_libreta = :numLibreta")

    Estudiante findByNumLibreta(@Param("numLibreta") Long numLibreta);

    @Query(
                """
                SELECT new org.example.entrega3.DTOs.Request.EstudianteDTO(
                    e.genero,
                )
                FROM Estudiante e
                WHERE ( :nombre IS NULL OR e.nombre LIKE :nombre )
                AND ( :apellido IS NULL OR e.apellido LIKE :apellido )
                AND ( :email IS NULL OR e.email LIKE concat( '%', :email, '%') )
                """
            )
    List<EstudianteDTO> filterEstudiante(String genero, String orden);

    @Query("""
        SELECT e FROM Estudiante e
        WHERE (:genero IS NULL OR e.genero = :genero)
        """)
    List<EstudianteDTO> search(String genero, Sort orden);


}
