package Repository;


import Entity.Carrera;
import Entity.Estudiante;
import Entity.EstudianteCarrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

public class EstudianteImple implements Estudianteinter {

    //Singleton
    private static EstudianteImple instance = new EstudianteImple();

    public static EstudianteImple getInstance(){
        return instance;
    }

    public EstudianteImple(){

    }

    //a) dar de alta un estudiante
    @Override
    public void insertEstudiante(Estudiante estudiante) {
        EntityManager em = JPAutil.getEntityManager();

        em.getTransaction().begin();
        em.persist(estudiante);
        em.getTransaction().commit();

        em.close();

    }

    //b) matricular un estudiante en una carrera
    @Override
    public void matricularEstudiante(Long idEstudiante, Long idCarrera) {
        EntityManager em = JPAutil.getEntityManager();

        em.getTransaction().begin();

        Estudiante estudiante = em.find(Estudiante.class, idEstudiante);
        Carrera carrera = em.find(Carrera.class, idCarrera);


        EstudianteCarrera matricula = new EstudianteCarrera(estudiante, carrera, LocalDate.now());
        em.persist(matricula);
        em.getTransaction().commit();

        em.close();

    }

    //c) recuperar todos los estudiantes, y especificar algún criterio de ordenamiento simple
    @Override
    public List<Estudiante> getEstudiantesInOrder() {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<Estudiante> estudiantesInOrder =  em.createQuery(
                "SELECT e FROM Estudiante e " +
                        "ORDER BY e.apellido ASC, e.nombre ASC", Estudiante.class);

        List<Estudiante> resultado = estudiantesInOrder.getResultList();
        em.close();
        return resultado;
    }

    //d) recuperar un estudiante, en base a su número de libreta universitaria.

    public Estudiante getEstudiantePorNumLibreta(Long numLibreta) {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<Estudiante> estudiantePorNumLib = em.createQuery(
                "SELECT e FROM Estudiante e " +
                        "WHERE e.num_libreta = :numLibreta ", Estudiante.class);

        estudiantePorNumLib.setParameter("numLibreta", numLibreta);

        Estudiante estudiante = estudiantePorNumLib.getSingleResult();

        em.getTransaction().commit();
        em.close();

        return estudiante;
    }

    // e) recuperar todos los estudiantes, en base a su género

    public List<Estudiante> getEstudiantesPorGenero(String generoSolicitado) {

        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<Estudiante> estudiantesPorGenero = em.createQuery(
                "SELECT e FROM Estudiante e " +
                        "WHERE e.genero = :generoSolicitado ", Estudiante.class);

        estudiantesPorGenero.setParameter("generoSolicitado", generoSolicitado);

        List<Estudiante> resultado = estudiantesPorGenero.getResultList();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }

    // g) recuperar los estudiantes de una determinada carrera, filtrado por ciudad de residencia
    @Override
    public List<Estudiante> getEstudiantesPorCarreraYCiudad(Long idCarrera, String ciudad) {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<Estudiante> query = em.createQuery(
                "SELECT e FROM EstudianteCarrera ec " +
                        "JOIN ec.estudiante e " +
                        "WHERE ec.carrera.idCarrera = :idCarrera " +
                        "AND e.ciudadResidencia = :ciudad", Estudiante.class);

        query.setParameter("idCarrera", idCarrera);
        query.setParameter("ciudad", ciudad);

        List<Estudiante> resultado = query.getResultList();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }


}
