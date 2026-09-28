package Utils;
import com.opencsv.CSVReader;
import Entity.Carrera;
import Entity.Estudiante;
import Entity.EstudianteCarrera;
import Factory.JPAutil;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class CargaDedatosCsv {
    public void insertarEstudianteCarreraDesdeCSV(String rutaArchivo) {
        EntityManager em = JPAutil.getEntityManager();

        try (CSVReader reader = new CSVReader(new FileReader(rutaArchivo))) {
            String[] linea;
            reader.readNext();

            em.getTransaction().begin();

            while ((linea = reader.readNext()) != null) {
                try {
                    Long dni = Long.valueOf(linea[1].trim());
                    System.out.println("DNI:" + " " + dni);
                    if(dni<1000){
                        System.out.println("DNI no existe:" + " " + dni);
                        continue;
                    }

                    Estudiante resultado = em
                            .createQuery("SELECT e FROM Estudiante e WHERE e.dni = :dni", Estudiante.class)
                            .setParameter("dni", dni)
                            .getSingleResult();
                    if (resultado==null) {
                        System.out.println("ERROR: No se encontró estudiante con DNI: " + dni);
                        continue;
                    }

                    Long idCarrera = Long.valueOf(linea[2].trim());
                    Carrera carrera = em.find(Carrera.class, idCarrera);
                    if (carrera == null) {
                        System.out.println("ERROR: No se encontró carrera con ID: " + linea[2]);
                        continue;
                    }

                    int anio1 =  Integer.parseInt(linea[3]);
                    LocalDate fecha1= LocalDate.of(anio1, 1, 1);

                    EstudianteCarrera ec = new EstudianteCarrera(resultado,carrera,fecha1);

//
//                    int anioGraduacion = Integer.parseInt(linea[4]) ;
//                    LocalDate fecha = LocalDate.of(anioGraduacion, 1, 1);
//                    ec.setFechaGraduacion(fecha);

                    String campoGraduacion = linea[4].trim();
                    if (!"0".equals(campoGraduacion)) {
                        int anioGrad = Integer.parseInt(campoGraduacion);
                        ec.setFechaGraduacion(LocalDate.of(anioGrad, 1, 1));
                    } else {
                        ec.setFechaGraduacion(null); // sigue cursando
                    }
                    //em.persist(ec);

                    em.merge(ec);//Se usa para entidades con ID asignado manualmente:


                }
                catch (NoResultException e){
                    System.out.println("No existe el estudiante con DNI: " + linea[0]);
                }

                catch (Exception lineException) {

                    System.out.println("Error procesando línea: " + String.join(",", linea));
                    lineException.printStackTrace();
                }

            }

            em.getTransaction().commit();
            System.out.println("Transacción completada exitosamente 510");


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
                est.setNum_libreta((Long.valueOf(linea[6])));
                est.setNombre(linea[1]);
                est.setApellido(linea[2]);

                var Edad= Integer.parseInt(linea[3]);
                LocalDate fechaPasada = LocalDate.now().minusYears(Edad);

                est.setFechaNacimiento(fechaPasada);
                est.setGenero(linea[4]);
                est.setCiudadResidencia(linea[5]);
                est.setDni(Long.valueOf(linea[0]));
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