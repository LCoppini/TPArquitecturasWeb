package org.example.entrega3.services;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.CarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Response.CarreraResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteCarreraResponseDTO;
import org.example.entrega3.Exception.CarreraException;
import org.example.entrega3.Exception.NotFoundException;
import org.example.entrega3.Repository.CarreraRepository;
import org.example.entrega3.Repository.EstudianteCarreraRepository;
import org.example.entrega3.Repository.EstudianteRepository;
import org.example.entrega3.model.Carrera;
import org.example.entrega3.model.Estudiante;
import org.example.entrega3.model.EstudianteCarrera;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service("Carrera servicio")
@RequiredArgsConstructor
public class CarreraServices {

    private final CarreraRepository carreraRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstudianteCarreraRepository estudianteCarreraRepository;

    // CRUD

    @Transactional(readOnly = true)
    public List<CarreraResponseDTO> findAll() {
        return this.carreraRepository.findAll().stream().map(CarreraResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public CarreraResponseDTO findById(Long id) {
        return new CarreraResponseDTO(this.buscarCarrera(id));
    }

    @Transactional
    public CarreraResponseDTO save(CarreraDTO request) {
        if (request.id() == null)
            throw new CarreraException("El id de la carrera es un campo obligatorio.");
        if (this.carreraRepository.existsById(request.id()))
            throw new CarreraException("Ya existe una carrera con id " + request.id() + ".");

        final var carrera = new Carrera(request.nombreCarrera(), request.id(), request.duracion());
        return new CarreraResponseDTO(this.carreraRepository.save(carrera));
    }

    @Transactional
    public CarreraResponseDTO update(Long id, CarreraDTO request) {
        final var carrera = this.buscarCarrera(id);
        carrera.setNombreCarrera(request.nombreCarrera());
        carrera.setDuracion(request.duracion());
        return new CarreraResponseDTO(this.carreraRepository.save(carrera));
    }

    @Transactional
    public void delete(Long id) {
        final var carrera = this.buscarCarrera(id);
        if (!carrera.getEstudiantes().isEmpty())
            throw new CarreraException("No se puede eliminar la carrera " + id + " porque tiene estudiantes inscriptos.");
        this.carreraRepository.delete(carrera);
    }

    // b) matricular un estudiante en una carrera

    @Transactional
    public EstudianteCarreraResponseDTO matricular(Long idCarrera, EstudianteCarreraDTO request) {

        final Carrera carrera = this.buscarCarrera(idCarrera);
        final Estudiante estudiante = this.estudianteRepository.findById(request.numLibreta())
                .orElseThrow(() -> new NotFoundException(
                        "El estudiante con libreta " + request.numLibreta() + " no existe."));

        final boolean matriculado = estudiante.getCarreras().stream()
                .anyMatch(ec -> ec.getCarrera().getIdCarrera().equals(idCarrera));
        if (matriculado)
            throw new CarreraException("El estudiante " + estudiante.getNum_libreta()
                    + " ya esta matriculado en la carrera " + idCarrera + ".");

        final LocalDate fechaInscripcion = request.fechaInscripcion() != null
                ? request.fechaInscripcion() : LocalDate.now();

        final var matricula = new EstudianteCarrera(estudiante, carrera, fechaInscripcion);
        matricula.setFechaGraduacion(request.fechaGraduacion());

        return new EstudianteCarreraResponseDTO(this.estudianteCarreraRepository.save(matricula));
    }

    // helpers

    private Carrera buscarCarrera(Long id) {
        return this.carreraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La carrera con id " + id + " no existe."));
    }
}
