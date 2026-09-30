package Repository;

import DTO.ReporteCarreraDTO;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class EstudianteCarreraImple implements EstudianteCarreraInter {

    // Singleton
    private static EstudianteCarreraImple instance = new EstudianteCarreraImple();

    public EstudianteCarreraImple() {
    }

    private EstudianteCarreraImple getInstance() {
        return instance;
    }

    // 3) reporte de carreras: inscriptos y egresados por año, carreras ordenadas alfabéticamente
    @Override
    public List<ReporteCarreraDTO> getReporteInscriptosYEgresadosPorAnio() {

        String sql = """
            SELECT
                c.nombreCarrera,
                a.anio,
                SUM(a.inscriptos) AS cantidad_inscriptos,
                SUM(a.egresados)  AS cantidad_egresados
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
        """;

        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        Query query = em.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> filas = query.getResultList();

        em.getTransaction().commit();
        em.close();

        List<ReporteCarreraDTO> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            String nombreCarrera = (String) fila[0];
            int anio = ((Number) fila[1]).intValue();
            long cantInscriptos = ((Number) fila[2]).longValue();
            long cantEgresados = ((Number) fila[3]).longValue();

            resultado.add(new ReporteCarreraDTO(nombreCarrera, anio, cantInscriptos, cantEgresados));
        }

        return resultado;
    }
}