package org.example;

import Entity.Carrera;
import Entity.Estudiante;
import Entity.EstudianteCarrera;
import Repository.CarreraImple;
import Repository.EstudianteCarreraImple;
import Repository.EstudianteImple;
import Utils.CargaDedatosCsv;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        final var carga = new CargaDedatosCsv();

//        carga.insertarEstudianteDesdeCSV("Entrega2/src/main/resources/data/estudiantes.csv");
//        carga.insertarCarreraDesdeCSV("Entrega2/src/main/resources/data/carreras.csv");
//        carga.insertarEstudianteCarreraDesdeCSV("Entrega2/src/main/resources/data/estudianteCarrera.csv");
//
//        System.out.println("Carga Inicial");

//            Estudiante e = new Estudiante(127000L, "pablo", "martinez", LocalDate.now(), "Hombre", 67540230L, "tandil");
//            EstudianteImple.getInstance().insertEstudiante(e);
//

//            EstudianteCarrera ec = new EstudianteCarrera(e, c, LocalDate.now());
            //EstudianteImple.getInstance().matricularEstudiante(55783L, 5L);


//            var servicio = new EstudianteImple();
//            List<Estudiante> estudiantes = servicio.getEstudiantesInOrder();
//
//            System.out.println("Estudiantes ordenados por apellido y nombre:");
//            for (Estudiante e : estudiantes) {
//                System.out.println(e.getApellido() + ", " + e.getNombre());
//            }
//             var servicio = new EstudianteImple();
//             Estudiante estudiante = servicio.getEstudiantePorNumLibreta(43523L);
//
//             System.out.println(estudiante.getGenero() + "," + estudiante.getApellido());





    }
}