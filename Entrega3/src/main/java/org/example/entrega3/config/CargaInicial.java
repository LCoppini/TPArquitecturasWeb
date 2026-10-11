package org.example.entrega3.config;

import org.example.entrega3.services.CargaDeDatosServices;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
public class CargaInicial implements CommandLineRunner {

    private final CargaDeDatosServices service;

    public CargaInicial(CargaDeDatosServices service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        // Poné los CSV en src/main/resources/csv/
        service.cargarCarreras(leer("data/carreras.csv"));
        service.cargarEstudiantes(leer("data/estudiantes.csv"));
        service.cargarEstudianteCarrera(leer("data/estudianteCarrera.csv"));
    }

    private Reader leer(String ruta) throws IOException {
        return new InputStreamReader(new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8);
    }
}
