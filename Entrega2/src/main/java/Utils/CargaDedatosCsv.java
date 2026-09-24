package Utils;
import com.opencsv.CSVReader;
import Entity.Carrera;
import Entity.Estudiante;
import Entity.EstudianteCarrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.Period;

public class CargaDedatosCsv {
    public void insertarEstudianteCarreraDesdeCSV(String rutaArchivo) {
        EntityManager em = JPAutil.getEntityManager();

        try (CSVReader reader = new CSVReader(new FileReader(rutaArchivo))) {
            String[] linea;
            reader.readNext();

            em.getTransaction().begin();

            while ((linea = reader.readNext()) != null) {
                try {
                    EstudianteCarrera ec = new EstudianteCarrera();

                    System.out.println("Procesando línea: " + String.join(",", linea));
                    System.out.println("Buscando estudiante con DNI: " + linea[1]);
                    System.out.println("Buscando carrera con ID: " + linea[2]);

                    Estudiante est = em.find(Estudiante.class, linea[1]);
                    if (est == null) {
                        System.out.println("ERROR: No se encontró estudiante con DNI: " + linea[1]);
                    }

                    Carrera carrera = em.find(Carrera.class, Integer.parseInt(linea[2]));
                    if (carrera == null) {
                        System.out.println("ERROR: No se encontró carrera con ID: " + linea[2]);
                    }

                    ec.setEstudiante(est);
                    ec.setCarrera(carrera);
                    int anio1 =  Integer.parseInt(linea[4]);
                    LocalDate fecha1= LocalDate.of(anio1, 1, 1);

                    ec.setFechaDeinscripcion(fecha1);
                    int anio = Integer.parseInt(linea[5]) ;

                    LocalDate fecha = LocalDate.of(anio, 1, 1);
                    ec.setFechaGraduacion(fecha);

                    em.persist(ec);
                    System.out.println("EstudianteCarrera persistido correctamente");

                } catch (Exception lineException) {
                    System.out.println("Error procesando línea: " + String.join(",", linea));
                    lineException.printStackTrace();
                }
            }
            em.getTransaction().commit();
            System.out.println("Transacción completada exitosamente");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void insertarEstudianteDesdeCSV(String rutaArchivo) {
        EntityManager em = JPAutil.getEntityManager();

        try (CSVReader reader = new CSVReader(new FileReader(rutaArchivo))) {
            String[] linea;
            reader.readNext();

            em.getTransaction().begin();

            while ((linea = reader.readNext()) != null) {
                Estudiante est = new Estudiante();
                est.setNum_libreta((Long.valueOf(linea[0])));
                est.setNombre(linea[1]);
                est.setApellido(linea[2]);

                var Edad= Integer.parseInt(linea[3]);
                LocalDate fechaPasada = LocalDate.now().minusYears(Edad);

                est.setFechaNacimiento(fechaPasada);
                est.setGenero(linea[4]);
                est.setCiudadResidencia(linea[5]);
                est.setDni(Integer.parseInt(linea[6]));
                em.persist(est);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void insertarCarreraDesdeCSV(String rutaArchivo) {
        EntityManager em = JPAutil.getEntityManager();

        try (CSVReader reader = new CSVReader(new FileReader(rutaArchivo))) {
            String[] linea;
            reader.readNext();

            em.getTransaction().begin();

            while ((linea = reader.readNext()) != null) {
                Carrera carrera = new Carrera();
                carrera.setIdCarrera((Long.valueOf(linea[0])));
                carrera.setNombreCarrera(linea[1]);
                carrera.setDuracion(Integer.parseInt(linea[2]));
                em.persist(carrera);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}