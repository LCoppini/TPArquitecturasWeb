package org.example;

import Repository.CarreraImple;
import Repository.EstudianteImple;
import Utils.CargaDedatosCsv;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        System.out.println("Working directory: " + System.getProperty("user.dir"));
        final var carga = new CargaDedatosCsv();

//        carga.insertarEstudianteDesdeCSV("Entrega2/src/main/resources/data/estudiantes.csv");
//        carga.insertarCarreraDesdeCSV("Entrega2/src/main/resources/data/carreras.csv");
//        carga.insertarEstudianteCarreraDesdeCSV("Entrega2/src/main/resources/data/estudianteCarrera.csv");

        System.out.println("Carga Inicial");

        EstudianteImple estudianteRepo = new EstudianteImple();

        System.out.println("\n--- d) Estudiante por libreta ---");
        System.out.println(estudianteRepo.getEstudiantePorNumLibreta(34978L));

        System.out.println("\n--- g) Estudiantes por carrera y ciudad ---");
        estudianteRepo.getEstudiantesPorCarreraYCiudad(15L, "Jiaoyuan").forEach(System.out::println);

        System.out.println("\n--- f) Carreras por cantidad de inscriptos ---");
        new CarreraImple().getCarrerasPorCantidadInscriptos().forEach(System.out::println);
    }
}