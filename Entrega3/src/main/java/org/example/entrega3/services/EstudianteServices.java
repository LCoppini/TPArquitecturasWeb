package org.example.entrega3.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.DTOs.Request.EstudianteSearchDTO;
import org.example.entrega3.DTOs.Response.EstudianteResponseDTO;
import org.example.entrega3.Exception.EstudianteException;
import org.example.entrega3.model.Carrera;
import org.example.entrega3.model.EstudianteCarrera;
import org.example.entrega3.repository.CarreraRepository;
import org.example.entrega3.repository.EstudianteRepository;
import org.example.entrega3.model.Estudiante;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("Estudiante servicio")
@RequiredArgsConstructor
@Transactional
public class EstudianteServices {

    private final EstudianteRepository estudianteRepositorio;
    private final CarreraRepository carreraRepositorio;

    //CRUD
    //a-dar de alta un estudiante
    //d-recuperar un estudiante en base a su num_libreta(Ya realizado)


    //e-recuperar todos los estudiante en base a su genero(Realizado)

    //g-recuperar los estudiantes de una determinada carrera filtrado por ciudades de residencia

    @Transactional(readOnly = true)
    public List<EstudianteResponseDTO> findAll() throws Exception {
        return estudianteRepositorio.findAll().stream().map(EstudianteResponseDTO::new).toList(); //por cada est crea un new DTO
    }
    //c
    @Transactional(readOnly = true)
    public List<EstudianteDTO> search(EstudianteSearchDTO request, Sort orden){
        if (request.getGenero() == null || request.getGenero().isEmpty()){
            request.setGenero(null);
        }
        if (request.getCiudad() == null || request.getCiudad().isEmpty())
            request.setCiudad(null);

        return estudianteRepositorio.search(request.getGenero(),orden);

    }
    //d-recuperar un estudiante en base a su num_libreta
    @Transactional(readOnly = true)
    public EstudianteResponseDTO findByNumLibreta(Long numLibreta) {
        Estudiante e = estudianteRepositorio.findByNumLibreta(numLibreta);
        if (e == null) {
            throw new EstudianteException("Estudiante no encontrado");
        }
        return new EstudianteResponseDTO(e);
    }

    @Transactional(readOnly = true)
    public Estudiante findById(Long id) throws Exception {
        return null;
    }

    public void save(EstudianteDTO entity) throws Exception {
        Estudiante estudiante = new Estudiante(entity.num_libreta(), entity.nombre(),
                entity.apellido(), entity.fecha_nacimiento(), entity.genero(),
                entity.dni(), entity.ciudad_residencia());

        for (EstudianteCarreraDTO ecDto : entity.carreras()) {
            Carrera carrera = carreraRepositorio.findById(ecDto.idCarrera())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No existe la carrera " + ecDto.idCarrera()));

            EstudianteCarrera ec = new EstudianteCarrera(estudiante, carrera, ecDto.fechaInscripcion());
            ec.setFechaGraduacion(ecDto.fechaGraduacion());   // puede ser null
            estudiante.getCarreras().add(ec);
        }
        estudianteRepositorio.save(estudiante);
    }

    public Estudiante update(Long id, Estudiante entity) throws Exception {
        return null;
    }

    public boolean delete(Long id) throws Exception {
        return false;
    }
}
