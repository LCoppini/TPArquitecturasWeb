package org.example.entrega3.services;


import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.Repository.EstudianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("Carrera servicio")
@RequiredArgsConstructor
@Transactional
public class CarreraServices {

    private final EstudianteRepository estudianteRepository;

    //CRUD completo

    //b-Matricualar un estuadiante a una carrera

    public List<EstudianteDTO> findAllEstudiantes(){

        return null;
    }
}
