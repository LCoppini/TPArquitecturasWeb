package org.example.entrega3.services;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.CarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.DTOs.Request.EstudianteSearchDTO;
import org.example.entrega3.DTOs.Response.CarreraInscriptosResponseDTO;
import org.example.entrega3.DTOs.Response.CarreraResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteCarreraResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteResponseDTO;
import org.example.entrega3.Exception.CarreraException;
import org.example.entrega3.Exception.EstudianteException;
import org.example.entrega3.Exception.NotFoundException;
import org.example.entrega3.repository.CarreraRepository;
import org.example.entrega3.repository.EstudianteCarreraRepository;
import org.example.entrega3.repository.EstudianteRepository;
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
        Carrera c = this.carreraRepository.findById(id).orElse(null);
        if (c == null) {
            throw new CarreraException("Carrera con id: " + id +" no encontrada"); //CAMBIO
        }
        return new CarreraResponseDTO(c);
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
        Carrera carrera = carreraRepository.findById(id).orElse(null);
        if(carrera == null)
            throw new NotFoundException("El id: " + id +" de la carrera no existe.");
        carrera.setNombreCarrera(request.nombreCarrera());
        carrera.setDuracion(request.duracion());
        return new CarreraResponseDTO(this.carreraRepository.save(carrera));
    }

    @Transactional
    public CarreraResponseDTO delete(Long id) {
        Carrera carrera = carreraRepository.findById(id).orElse(null);

       if (!carrera.getEstudiantes().isEmpty() ){
            throw new CarreraException("No se puede eliminar la carrera " + id + " porque tiene estudiantes inscriptos.");
        }
        this.carreraRepository.delete(carrera);
        return new CarreraResponseDTO(carrera);
    }


    //f-Recuperar carreras con estudiantes incriptos y ordernar por cantdiad de inscriptos
    @Transactional(readOnly = true)
    public List<CarreraInscriptosResponseDTO> getCarreraConCantInscriptos() {
        return carreraRepository.getCarreraConCantInscriptos();
    }

    //g- recuperar los estudiantes de una determinada carrera, filtrado por ciudad de residencia.
    public List<EstudianteResponseDTO> getEstudianteByCiudad(Long idCarrera, EstudianteSearchDTO request) throws Exception {
        if (request.getCiudad() == null || request.getCiudad().isEmpty()){
            request.setCiudad(null);
        }
        return carreraRepository.getEstudianteByCiudad(idCarrera,request.getCiudad());

    }
}
