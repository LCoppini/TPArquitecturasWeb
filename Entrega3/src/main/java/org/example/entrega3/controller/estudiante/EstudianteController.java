package org.example.entrega3.controller.estudiante;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.services.EstudianteServices;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/estudiantes")
public class EstudianteController {

    private final EstudianteServices estudianteServicio;

    @GetMapping()
    public List<EstudianteDTO> getEstudianteBy(EstudianteSearchDTO request, Sort sort ) throws Exception {
        return estudianteServicio.search( request, sort );
    }


}
