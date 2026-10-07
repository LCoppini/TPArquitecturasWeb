package org.example.entrega3.controller.estudiante;

import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.services.EstudianteServices;
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
    public List<EstudianteDTO> getEstudianteBy(
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String ciudad) throws Exception {
        return estudianteServicio.search(orderBy, genero, ciudad);
    }

}
