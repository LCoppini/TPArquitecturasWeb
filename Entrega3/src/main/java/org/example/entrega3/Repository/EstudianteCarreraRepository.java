package org.example.entrega3.Repository;


import org.example.entrega3.DTOs.Response.ReporteResponseDTO;
import org.example.entrega3.model.EstudianteCarrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteCarreraRepository extends JpaRepository<EstudianteCarrera, Long> {

    //h- generar un reporte de las carreras, que para cada carrera incluya información de los
    //inscriptos y egresados por año. Se deben ordenar las carreras alfabéticamente, y
    //presentar los años de manera cronológica.


    @Query( value = """
            SELECT
                c.nombreCarrera,
                a.anio  As anio,
                SUM(a.inscriptos) AS cantidadInscriptos,
                SUM(a.egresados)  AS cantidadEgresados
            FROM (
                SELECT id_carrera, CAST(YEAR(fecha_inscripcion) AS SIGNED) AS anio, 1 AS inscriptos, 0 AS egresados
                FROM EstudianteCarrera
    
                UNION ALL
    
                SELECT id_carrera, CAST(YEAR(fecha_graduacion) AS SIGNED) AS anio, 0 AS inscriptos, 1 AS egresados
                FROM EstudianteCarrera
                WHERE fecha_graduacion IS NOT NULL
            ) a
            JOIN Carrera c ON c.idCarrera = a.id_carrera
            GROUP BY c.idCarrera, c.nombreCarrera, a.anio
            ORDER BY c.nombreCarrera ASC, a.anio ASC
            """, nativeQuery = true)
    List<ReporteProjetion> reporteCarreras();
}
