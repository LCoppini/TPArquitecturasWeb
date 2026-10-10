package org.example.entrega3.controller.carrera;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.CarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Response.CarreraInscriptosResponseDTO;
import org.example.entrega3.DTOs.Response.CarreraResponseDTO;
import org.example.entrega3.DTOs.Response.EstudianteCarreraResponseDTO;
import org.example.entrega3.services.CarreraServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraServices carreraServices;

    @GetMapping
    public List<CarreraResponseDTO> findAll() {
        return this.carreraServices.findAll();
    }

    @GetMapping("/{id}")
    public CarreraResponseDTO findById(@PathVariable Long id) {
        return this.carreraServices.findById(id);
    }

    @PostMapping
    public ResponseEntity<CarreraResponseDTO> save(@RequestBody @Valid CarreraDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.carreraServices.save(request));
    }

    @PutMapping("/{id}")
    public CarreraResponseDTO update(@PathVariable Long id, @RequestBody @Valid CarreraDTO request) {
        return this.carreraServices.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.carreraServices.delete(id);
        return ResponseEntity.noContent().build();
    }

    // b) matricular un estudiante en una carrera
    @PostMapping("/{id}/matricular")
    public ResponseEntity<EstudianteCarreraResponseDTO> matricular(@PathVariable Long id,
                                                                  @RequestBody @Valid EstudianteCarreraDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.carreraServices.matricular(id, request));
    }

    //f-REcuperar carreras con estudiantes incriptos y ordernar por cantdiad de inscriptos
    @GetMapping("/inscriptos")
    public List<CarreraInscriptosResponseDTO> getCarreraConCantInscriptos() {
        return carreraServices.getCarreraConCantInscriptos();
    }
}
