package org.example;

import Entity.Estudiante;
import Repository.CarreraImple;
import Repository.EstudianteCarreraImple;
import Repository.EstudianteImple;
import Utils.CargaDedatosCsv;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        final var carga = new CargaDedatosCsv();

//        carga.insertarEstudianteDesdeCSV("Entrega2/src/main/resources/data/estudiantes.csv");
//        carga.insertarCarreraDesdeCSV("Entrega2/src/main/resources/data/carreras.csv");
//        carga.insertarEstudianteCarreraDesdeCSV("Entrega2/src/main/resources/data/estudianteCarrera.csv");

//        System.out.println("Carga Inicial");

        EstudianteImple estudianteRepo = new EstudianteImple();

        System.out.println("\n--- a) Alta de estudiante ---");
        Estudiante nuevoEstudiante = new Estudiante();
        nuevoEstudiante.setNum_libreta(99999L);
        nuevoEstudiante.setDni(40123456L);
        nuevoEstudiante.setNombre("Juan");
        nuevoEstudiante.setApellido("Perez");
        nuevoEstudiante.setFechaNacimiento(LocalDate.now().minusYears(20));
        nuevoEstudiante.setGenero("Male");
        nuevoEstudiante.setCiudadResidencia("La Plata");

        estudianteRepo.insertEstudiante(nuevoEstudiante);
        System.out.println("Estudiante insertado");

        System.out.println("\n--- b) Matricular estudiante en carrera ---");
        estudianteRepo.matricularEstudiante(nuevoEstudiante.getNum_libreta(), 15L);
        System.out.println("Estudiante " + nuevoEstudiante.getNombre() + " " + nuevoEstudiante.getApellido() +
                " matriculado en la carrera con id 15");

        System.out.println("\n--- c) Estudiantes ordenados ---");
        estudianteRepo.getEstudiantesInOrder().forEach(System.out::println);

        System.out.println("\n--- d) Estudiante por libreta ---");
        System.out.println(estudianteRepo.getEstudiantePorNumLibreta(34978L));

        System.out.println("\n--- e) Estudiantes por genero ---");
        estudianteRepo.getEstudiantesPorGenero("Male").forEach(System.out::println);

        System.out.println("\n--- f) Carreras por cantidad de inscriptos ---");
        new CarreraImple().getCarrerasPorCantidadInscriptos().forEach(System.out::println);

        System.out.println("\n--- g) Estudiantes por carrera y ciudad ---");
        estudianteRepo.getEstudiantesPorCarreraYCiudad(15L, "Jiaoyuan").forEach(System.out::println);

        System.out.println("\n--- 3) Reporte de inscriptos y egresados por año ---");
        new EstudianteCarreraImple().getReporteInscriptosYEgresadosPorAnio().forEach(rep ->
                System.out.println(rep.getNombreCarrera() + " - " + rep.getAnio() +
                        ": " + rep.getCantInscriptos() + " inscriptos, " + rep.getCantEgresados() + " egresados")
        );
    }
}