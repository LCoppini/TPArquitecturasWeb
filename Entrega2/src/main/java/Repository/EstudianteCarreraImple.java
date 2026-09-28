package Repository;


import DTO.ReporteCarreraDTO;
import Entity.Carrera;
import Entity.EstudianteCarrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.*;

public class EstudianteCarreraImple implements EstudianteCarreraInter {
    //Singleton
    private static EstudianteCarreraImple instance = new EstudianteCarreraImple();

    private EstudianteCarreraImple getInstance() {
        return instance;
    }

    public EstudianteCarreraImple() {

    }

    // 3) reporte de carreras: inscriptos y egresados por año, carreras ordenadas alfabéticamente

    // NO vaaa esta malll

    @Override
    public List<ReporteCarreraDTO> getReporteInscriptosYEgresadosPorAnio() {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<EstudianteCarrera> query = em.createQuery(
                "SELECT ec FROM EstudianteCarrera ec " +
                        "ORDER BY ec.carrera.nombreCarrera ASC", EstudianteCarrera.class);

        List<EstudianteCarrera> matriculas = query.getResultList();
        em.getTransaction().commit();
        em.close();

        // carrera -> (anio -> [inscriptos, egresados])
        Map<Carrera, TreeMap<Integer, long[]>> acumulado = new LinkedHashMap<>();

        for (EstudianteCarrera ec : matriculas) {
            Carrera carrera = ec.getCarrera();
            int anioInscripcion = ec.getFechaDeinscripcion().getYear();

            if (!acumulado.containsKey(carrera)) {
                acumulado.put(carrera, new TreeMap<>());
            }
            Map<Integer, long[]> porAnio = acumulado.get(carrera);

            if (!porAnio.containsKey(anioInscripcion)) {
                porAnio.put(anioInscripcion, new long[2]);
            }
            porAnio.get(anioInscripcion)[0]++;

            if (ec.estaGraduado()) {
                int anioGraduacion = ec.getFechaGraduacion().getYear();
                if (!porAnio.containsKey(anioGraduacion)) {
                    porAnio.put(anioGraduacion, new long[2]);
                }
                porAnio.get(anioGraduacion)[1]++;
            }
        }

        List<ReporteCarreraDTO> reporte = new ArrayList<>();
        for (Map.Entry<Carrera, TreeMap<Integer, long[]>> entradaCarrera : acumulado.entrySet()) {
            String nombreCarrera = entradaCarrera.getKey().getNombreCarrera();

            for (Map.Entry<Integer, long[]> entradaAnio : entradaCarrera.getValue().entrySet()) {
                int anio = entradaAnio.getKey();
                long inscriptos = entradaAnio.getValue()[0];
                long egresados = entradaAnio.getValue()[1];

                reporte.add(new ReporteCarreraDTO(nombreCarrera, anio, inscriptos, egresados));
            }
        }
        return reporte;
    }
}