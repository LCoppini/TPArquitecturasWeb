package org.example.entrega3;

import org.example.entrega3.services.CargaDeDatosServices;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class Entrega3Application {

    public static void main(String[] args) {
        SpringApplication.run(Entrega3Application.class, args);
    }
    @Bean
    CommandLineRunner CargaInicial(CargaDeDatosServices service) {
        return args -> {
            service.cargarCarreras(leer("data/carreras.csv"));
            service.cargarEstudiantes(leer("data/estudiantes.csv"));
            service.cargarEstudianteCarrera(leer("data/estudianteCarrera.csv"));
            System.out.println("Carga de datos finalizada");
        };
    }

    private static Reader leer(String ruta) throws IOException {
        return new InputStreamReader(
                new ClassPathResource(ruta).getInputStream(), StandardCharsets.UTF_8);
    }

}
