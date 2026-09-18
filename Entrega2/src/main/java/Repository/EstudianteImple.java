package Repository;


import Entity.Carrera;
import Entity.Estudiante;
import Entity.EstudianteCarrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import java.time.LocalDate;

public class EstudianteImple implements Estudianteinter {

    @Override
    public void insertEstudiante(Estudiante estudiante) {
        EntityManager em = JPAutil.getEntityManager();

        em.getTransaction().begin();
        em.persist(estudiante);
        em.getTransaction().commit();

        em.close();

    }

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
}
