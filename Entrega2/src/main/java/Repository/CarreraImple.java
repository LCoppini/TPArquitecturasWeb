package Repository;

import DTO.CarreraDTO;
import Entity.Carrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class CarreraImple implements CarreraInter {
    // Singleton
    private static CarreraImple instance = new CarreraImple();

    private CarreraImple getInstance(){
        return instance;
    }

    public CarreraImple(){

    }

    // f) recuperar las carreras con estudiantes inscriptos, y ordenar por cantidad de inscriptos
    @Override
    public List<CarreraDTO> getCarrerasPorCantidadInscriptos() {
        EntityManager em = JPAutil.getEntityManager();
        em.getTransaction().begin();

        TypedQuery<CarreraDTO> query = em.createQuery(
                "SELECT new DTO.CarreraDTO(ec.carrera.idCarrera, ec.carrera.nombreCarrera, ec.carrera.duracion, COUNT(ec)) " +
                        "FROM EstudianteCarrera ec " +
                        "GROUP BY ec.carrera.idCarrera, ec.carrera.nombreCarrera, ec.carrera.duracion " +
                        "ORDER BY COUNT(ec) DESC", CarreraDTO.class);

        List<CarreraDTO> resultado = query.getResultList();

        em.getTransaction().commit();
        em.close();

        return resultado;
    }

}
