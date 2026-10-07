package org.example.entrega3.services;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Response.EstudianteResponseDTO;
import org.example.entrega3.Repository.EstudianteRepository;
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

    //CRUD
    //a-dar de alta un estudiante

    //d-recuperar un estudiante en base a su num_libreta

    //e-recuperar todos los estudiante en base a su genero

    //g-recuperar los estudiantes de una determinada carrera filtrado por ciudades de residencia

    @Transactional(readOnly = true)
    public List<EstudianteResponseDTO> findAll() throws Exception {
        return estudianteRepositorio.findAll().stream().map(EstudianteResponseDTO::new).toList(); //por cada est crea un new DTO
    }
    //c
    @Transactional(readOnly = true)
    public List<EstudianteResponseDTO> findAll(String order) throws Exception {
        if (order == null)
            return this.findAll();

        Sort sort = Sort.by(order);
        return estudianteRepositorio.findAll(sort).stream().map(EstudianteResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public Estudiante findById(Long id) throws Exception {
        return null;
    }

    public Estudiante save(Estudiante entity) throws Exception {
        return null;
    }

    public Estudiante update(Long id, Estudiante entity) throws Exception {
        return null;
    }

    public boolean delete(Long id) throws Exception {
        return false;
    }
}
