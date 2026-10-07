package org.example.entrega3.services;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.EstudianteDTO;
import org.example.entrega3.Repository.EstudianteRepository;
import org.example.entrega3.model.Estudiante;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service("Estudiante servicio")
@RequiredArgsConstructor
@Transactional
public class EstudianteServices {

    private final EstudianteRepository estudianteRepositorio;

    @Transactional(readOnly = true)
    public List<EstudianteDTO> findAll() throws Exception {
        return estudianteRepositorio.findAll().stream().map(EstudianteDTO::new).toList(); //por cada est crea un new DTO
    }
    @Transactional(readOnly = true)
    public List<EstudianteDTO> findAll(String order) throws Exception {
        List<EstudianteDTO> estudiantes = null;

        if (order == null)
            estudiantes =  this.findAll();



        return estudiantes.stream().map(EstudianteDTO::new).toList();
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
