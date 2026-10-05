package org.example.entrega3.controller.estudiante;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.EstudianteDTO;
import org.example.entrega3.services.EstudianteServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/equipos")
public class EstudianteController {

    private final EstudianteServices estudianteServicio;

    @GetMapping("")
    public List<EstudianteDTO> getEstudiantes() throws Exception {
        return estudianteServicio.findAll();
    }

}
