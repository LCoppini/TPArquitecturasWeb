package Repository;

import Entity.Carrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class CarreraImple implements CarreraInter {
    // Singleton
    private static CarreraImple instance = new CarreraImple();

    public static CarreraImple getInstance(){
        return instance;
    }

    public CarreraImple(){

    }

    // f) recuperar las carreras con estudiantes inscriptos, y ordenar por cantidad de inscriptos
    @Override
    public List<Carrera> getCarrerasPorCantidadInscriptos() {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<Carrera> carrerasConInscriptos = em.createQuery(
                "SELECT ec.carrera FROM EstudianteCarrera ec " +
                        "GROUP BY ec.carrera " +
                        "ORDER BY COUNT(ec) DESC", Carrera.class);

        List<Carrera> resultado = carrerasConInscriptos.getResultList();

        em.close();

        return resultado;
    }


}
