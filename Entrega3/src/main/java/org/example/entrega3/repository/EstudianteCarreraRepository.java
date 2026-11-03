package org.example.entrega3.repository;


import org.example.entrega3.model.EstudianteCPK;
import org.example.entrega3.model.EstudianteCarrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteCarreraRepository extends JpaRepository<EstudianteCarrera, EstudianteCPK> {

    //h- generar un reporte de las carreras, que para cada carrera incluya información de los
    //inscriptos y egresados por año. Se deben ordenar las carreras alfabéticamente, y
    //presentar los años de manera cronológica.

    @Query( value = """
            SELECT
                    c.nombre_carrera,
                            c.id_carrera,
                            a.anio  As anio,
                            CAST(SUM(a.inscriptos) AS SIGNED) AS cantidadDeInscriptos,
                            CAST(SUM(a.egresados) AS SIGNED)  AS cantidadDeEgresados
                        FROM (
                            SELECT id_carrera, CAST(YEAR(fecha_inscripcion) AS SIGNED) AS anio, 1 AS inscriptos, 0 AS egresados
                            FROM estudiante_carrera

                            UNION ALL

                            SELECT id_carrera, CAST(YEAR(fecha_graduacion) AS SIGNED) AS anio, 0 AS inscriptos, 1 AS egresados
                            FROM estudiante_carrera
                            WHERE fecha_graduacion IS NOT NULL
                        ) a
                        JOIN carrera c ON c.id_carrera = a.id_carrera
                        GROUP BY c.id_carrera, c.nombre_carrera, a.anio
                        ORDER BY c.nombre_carrera ASC, a.anio ASC
            """, nativeQuery = true)
    List<ReporteProjection> reporteCarreras();



}
