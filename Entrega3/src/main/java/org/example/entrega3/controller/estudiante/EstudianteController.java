package org.example.entrega3.controller.estudiante;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.CarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteDTO;
import org.example.entrega3.DTOs.Request.EstudianteSearchDTO;
import org.example.entrega3.DTOs.Response.CarreraResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteCarreraResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteResponseDTO;
import org.example.entrega3.model.EstudianteCarrera;
import org.example.entrega3.repository.EstudianteRepository;
import org.example.entrega3.services.EstudianteCarreraService;
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

    private final EstudianteServices estudianteService;
    private final EstudianteCarreraService estudianteCarreraService;

    //CRUD
    @GetMapping()
    public List<EstudianteDTO> getEstudianteBy(EstudianteSearchDTO request, Sort sort ) throws Exception {
        return estudianteService.search( request, sort );
    }
    //d-recuperar un estudiante en base a su num_libreta
    @GetMapping("/{numLibreta}")
    public EstudianteResponseDTO getEstudianteByNumLibreta(@PathVariable Long numLibreta) throws Exception {
        return estudianteService.findByNumLibreta(numLibreta);
    }

    @PostMapping
    public ResponseEntity<Void> saveEstudiante(@RequestBody @Valid EstudianteDTO request) throws Exception {
        this.estudianteService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();  // ← 201 CREADO
    }

    // b) matricular un estudiante en una carrera
    @PostMapping("/{id}/matricular")
    public ResponseEntity<Void> matricular(@PathVariable Long id, @RequestBody @Valid EstudianteCarreraDTO request) {
        this.estudianteCarreraService.matricular(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();  // ← 201 CREADO
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstudianteResponseDTO>  update(@PathVariable Long id, @RequestBody @Valid EstudianteDTO request) throws Exception {
        EstudianteResponseDTO estudianteActualizado = this.estudianteService.update(id, request);
        return ResponseEntity.ok(estudianteActualizado);  // ← 200 OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EstudianteResponseDTO> delete(@PathVariable Long id) throws Exception {
        EstudianteResponseDTO estudianteEliminado = this.estudianteService.delete(id);
        return ResponseEntity.ok(estudianteEliminado);  // ← 200 OK
    }



}
