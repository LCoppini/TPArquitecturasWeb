package Repository;


import Entity.Carrera;
import Entity.Estudiante;
import Entity.EstudianteCarrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EstudianteImple implements Estudianteinter {

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


        EstudianteCarrera matricula = new EstudianteCarrera(estudiante, carrera);
        matricula.setFechaDeinscripcion(LocalDate.now());
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

        em.getTransaction().commit();
        em.close();
        return estudiantesInOrder.getResultList();
    }


}
