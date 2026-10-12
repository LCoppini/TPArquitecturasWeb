package org.example.entrega3.repository;

import org.example.entrega3.DTOs.Request.CarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.DTOs.Request.EstudianteSearchDTO;
import org.example.entrega3.DTOs.Response.CarreraInscriptosResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteResponseDTO;
import org.example.entrega3.model.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@Repository
public interface CarreraRepository extends JpaRepository<Carrera, Long> {
    //f-REcuperar carreras con estudiantes incriptos y ordernar por cantdiad de inscriptos

    @Query("""
    SELECT new org.example.entrega3.DTOs.Response.CarreraInscriptosResponseDTO(
        ec.carrera.idCarrera,
        ec.carrera.nombreCarrera,
        ec.carrera.duracion,
        COUNT(ec))
    FROM EstudianteCarrera ec
    GROUP BY ec.carrera.idCarrera, ec.carrera.nombreCarrera, ec.carrera.duracion
    ORDER BY COUNT(ec) DESC
    """)
    List<CarreraInscriptosResponseDTO> getCarreraConCantInscriptos();

    //g) recuperar los estudiantes de una determinada carrera, filtrado por ciudad de residencia
    @Query("""
        SELECT new org.example.entrega3.DTOs.Response.EstudianteResponseDTO(e)  FROM Carrera c
              JOIN EstudianteCarrera ec ON :idCarrera = ec.estudianteCPK.idCarrera
                    JOIN Estudiante e ON e.num_libreta = ec.estudianteCPK.idEstudiante
                          WHERE (:ciudad IS NULL OR e.ciudadResidencia = :ciudad )
                    
      """)
    List<EstudianteResponseDTO> getEstudianteByCiudad(Long idCarrera, String ciudad);
}
