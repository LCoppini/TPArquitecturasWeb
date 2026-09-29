package Repository;


import DTO.EstudianteDTO;
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

    private EstudianteImple getInstance(){
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
    public List<EstudianteDTO> getEstudiantesInOrder() {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<EstudianteDTO> query = em.createQuery(
                "SELECT new DTO.EstudianteDTO(e.num_libreta, e.nombre, e.apellido, e.fechaNacimiento, " +
                        "e.genero, e.dni, e.ciudadResidencia) " +
                        "FROM Estudiante e " +
                        "ORDER BY e.apellido ASC, e.nombre ASC", EstudianteDTO.class);

        List<EstudianteDTO> resultado = query.getResultList();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }

    //d) recuperar un estudiante, en base a su número de libreta universitaria.

    @Override
    public EstudianteDTO getEstudiantePorNumLibreta(Long numLibreta) {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<EstudianteDTO> query = em.createQuery(
                "SELECT new DTO.EstudianteDTO(e.num_libreta, e.nombre, e.apellido, e.fechaNacimiento, " +
                        "e.genero, e.dni, e.ciudadResidencia) " +
                        "FROM Estudiante e " +
                        "WHERE e.num_libreta = :numLibreta", EstudianteDTO.class);
        query.setParameter("numLibreta", numLibreta);

        EstudianteDTO resultado = query.getSingleResult();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }

    // e) recuperar todos los estudiantes, en base a su género

    @Override
    public List<EstudianteDTO> getEstudiantesPorGenero(String generoSolicitado) {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<EstudianteDTO> query = em.createQuery(
                "SELECT new DTO.EstudianteDTO(e.num_libreta, e.nombre, e.apellido, e.fechaNacimiento, " +
                        "e.genero, e.dni, e.ciudadResidencia) " +
                        "FROM Estudiante e " +
                        "WHERE e.genero = :generoSolicitado", EstudianteDTO.class);
        query.setParameter("generoSolicitado", generoSolicitado);

        List<EstudianteDTO> resultado = query.getResultList();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }

    // g) recuperar los estudiantes de una determinada carrera, filtrado por ciudad de residencia
    @Override
    public List<EstudianteDTO> getEstudiantesPorCarreraYCiudad(Long idCarrera, String ciudad) {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<EstudianteDTO> query = em.createQuery(
                "SELECT new DTO.EstudianteDTO(e.num_libreta, e.nombre, e.apellido, e.fechaNacimiento, " +
                        "e.genero, e.dni, e.ciudadResidencia) " +
                        "FROM EstudianteCarrera ec JOIN ec.estudiante e " +
                        "WHERE ec.carrera.idCarrera = :idCarrera AND e.ciudadResidencia = :ciudad",
                EstudianteDTO.class);
        query.setParameter("idCarrera", idCarrera);
        query.setParameter("ciudad", ciudad);

        List<EstudianteDTO> resultado = query.getResultList();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }


}
