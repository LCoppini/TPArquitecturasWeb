package org.example;

import Utils.CargaDedatosCsv;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        final var carga = new CargaDedatosCsv();

        carga.insertarEstudianteDesdeCSV("Entrega2/src/main/resources/data/estudiantes.csv");
        carga.insertarCarreraDesdeCSV("Entrega2/src/main/resources/data/carreras.csv");
        carga.insertarEstudianteCarreraDesdeCSV("Entrega2/src/main/resources/data/estudianteCarrera.csv");

        System.out.println("Carga Inicial");
    }
}