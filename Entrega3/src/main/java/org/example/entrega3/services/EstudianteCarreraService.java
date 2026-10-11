package org.example.entrega3.services;


import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Response.EstudianteCarreraResponseDTO;
import org.example.entrega3.DTOs.Response.ReporteResponseDTO;
import org.example.entrega3.Exception.CarreraException;
import org.example.entrega3.Exception.NotFoundException;
import org.example.entrega3.model.Carrera;
import org.example.entrega3.model.Estudiante;
import org.example.entrega3.model.EstudianteCarrera;
import org.example.entrega3.repository.CarreraRepository;
import org.example.entrega3.repository.EstudianteCarreraRepository;
import org.example.entrega3.repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service("EstudianteCarrera")
@RequiredArgsConstructor
@Transactional
public class EstudianteCarreraService {

    private final EstudianteCarreraRepository estudianteCarreraRepository;
    private final EstudianteRepository estudianteRepository;
    private final CarreraRepository carreraRepository;
    //CRUD

    // b) matricular un estudiante en una carrera

    @Transactional
    public void matricular(Long idCarrera, EstudianteCarreraDTO request) {

        Carrera carrera = this.buscarCarrera(idCarrera);
        Estudiante estudiante = this.estudianteRepository.findById(request.numLibreta())
                .orElseThrow(() -> new NotFoundException(
                        "El estudiante con libreta " + request.numLibreta() + " no existe."));

        boolean matriculado = estudiante.getCarreras().stream()
                .anyMatch(ec -> ec.getCarrera().getIdCarrera().equals(idCarrera));
        if (matriculado)
            throw new CarreraException("El estudiante " + estudiante.getNum_libreta()
                    + " ya esta matriculado en la carrera " + idCarrera + ".");

        LocalDate fechaInscripcion = request.fechaInscripcion() != null
                ? request.fechaInscripcion() : LocalDate.now();

        EstudianteCarrera matricula = new EstudianteCarrera(estudiante, carrera, fechaInscripcion);
        matricula.setFechaGraduacion(request.fechaGraduacion());

        new EstudianteCarreraResponseDTO(this.estudianteCarreraRepository.save(matricula));
    }

    // helpers

    private Carrera buscarCarrera(Long id) {
        return this.carreraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La carrera con id " + id + " no existe."));
    }

    //h
    //generar un reporte de las carreras, que para cada carrera incluya información de los
    //inscriptos y egresados por año. Se deben ordenar las carreras alfabéticamente, y
    //presentar los años de manera cronológica.
    @Transactional(readOnly = true)
    public List<ReporteResponseDTO> getReporte() {
        return estudianteCarreraRepository.reporteCarreras().stream()
                .map(p-> new ReporteResponseDTO(
                        p.getNombreCarrera(),
                        p.getAnio(),
                        p.getCantidadDeInscriptos(),
                        p.getCantidadDeEgresados()))
                .toList();
    }

}
