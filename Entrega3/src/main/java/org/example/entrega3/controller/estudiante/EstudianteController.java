package org.example.entrega3.controller.estudiante;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.DTOs.Request.EstudianteSearchDTO;
import org.example.entrega3.DTOs.Response.EstudianteResponseDTO;
import org.example.entrega3.services.EstudianteServices;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/estudiantes")
public class EstudianteController {

    private final EstudianteServices estudianteServicio;

    //CRUD
    @GetMapping()
    public List<EstudianteDTO> getEstudianteBy(EstudianteSearchDTO request, Sort sort ) throws Exception {
        return estudianteServicio.search( request, sort );
    }
    //d-recuperar un estudiante en base a su num_libreta
    @GetMapping("/{numLibreta}")
    public EstudianteResponseDTO getEstudianteByNumLibreta(@PathVariable Long numLibreta) throws Exception {
        return estudianteServicio.findByNumLibreta(numLibreta);
    }

    @PostMapping
    public void saveEstudiante(@RequestBody @Valid EstudianteDTO request) throws Exception {
         this.estudianteServicio.save(request);
    }



}
