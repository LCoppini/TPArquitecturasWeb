package org.example.entrega3.services;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.example.entrega3.model.Carrera;
import org.example.entrega3.model.Estudiante;
import org.example.entrega3.model.EstudianteCarrera;
import org.example.entrega3.repository.CarreraRepository;
import org.example.entrega3.repository.EstudianteCarreraRepository;
import org.example.entrega3.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CargaDeDatosServices {

    private final EstudianteRepository estudianteRepo;
    private final CarreraRepository carreraRepo;
    private final EstudianteCarreraRepository estudianteCarreraRepo;

    public CargaDeDatosServices(EstudianteRepository estudianteRepo,
                                CarreraRepository carreraRepo,
                                EstudianteCarreraRepository estudianteCarreraRepo) {
        this.estudianteRepo = estudianteRepo;
        this.carreraRepo = carreraRepo;
        this.estudianteCarreraRepo = estudianteCarreraRepo;
    }

    @Transactional
    public void cargarCarreras(Reader reader) throws IOException, CsvValidationException {
        try (CSVReader csv = new CSVReader(reader)) {
            csv.readNext(); // header
            String[] linea;
            List<Carrera> lote = new ArrayList<>();

            while ((linea = csv.readNext()) != null) {
                Carrera carrera = new Carrera();
                carrera.setIdCarrera(Long.valueOf(linea[0].trim()));
                carrera.setNombreCarrera(linea[1]);
                carrera.setDuracion(Integer.parseInt(linea[2].trim()));
                lote.add(carrera);
            }
            carreraRepo.saveAll(lote);
        }
    }

    @Transactional
    public void cargarEstudiantes(Reader reader) throws IOException, CsvValidationException {
        try (CSVReader csv = new CSVReader(reader)) {
            csv.readNext();
            String[] linea;
            List<Estudiante> lote = new ArrayList<>();

            while ((linea = csv.readNext()) != null) {
                Estudiante est = new Estudiante();
                est.setDni(Long.valueOf(linea[0].trim()));
                est.setNombre(linea[1]);
                est.setApellido(linea[2]);
                est.setFechaNacimiento(LocalDate.now().minusYears(Integer.parseInt(linea[3].trim())));
                est.setGenero(linea[4]);
                est.setCiudadResidencia(linea[5]);
                est.setNum_libreta(Long.valueOf(linea[6].trim()));
                lote.add(est);
            }
            estudianteRepo.saveAll(lote);
        }
    }

    @Transactional
    public void cargarEstudianteCarrera(Reader reader) throws IOException, CsvValidationException {
        try (CSVReader csv = new CSVReader(reader)) {
            csv.readNext();
            String[] linea;

            while ((linea = csv.readNext()) != null) {
                try {
                    Long dni = Long.valueOf(linea[1].trim());
                    if (dni < 1000) {
                        System.out.println("DNI no existe: " + dni);
                        continue;
                    }

                    Optional<Estudiante> estudiante = estudianteRepo.findById(dni);
                    if (estudiante.isEmpty()) {
                        System.out.println("No se encontró estudiante con  dni: " + dni);
                        continue;
                    }

                    Long idCarrera = Long.valueOf(linea[2].trim());
                    Optional<Carrera> carrera = carreraRepo.findById(idCarrera);
                    if (carrera.isEmpty()) {
                        System.out.println("No se encontró carrera con ID: " + idCarrera);
                        continue;
                    }

                    LocalDate inscripcion = LocalDate.of(Integer.parseInt(linea[3].trim()), 1, 1);
                    EstudianteCarrera ec = new EstudianteCarrera(estudiante.get(), carrera.get(), inscripcion);

                    String grad = linea[4].trim();
                    ec.setFechaGraduacion("0".equals(grad)
                            ? null
                            : LocalDate.of(Integer.parseInt(grad), 1, 1));

                    estudianteCarreraRepo.save(ec); // con ID asignado a mano hace merge

                } catch (NumberFormatException | DateTimeException e) {
                    System.out.println("Línea inválida: " + String.join(",", linea));
                }
            }
        }
    }
}