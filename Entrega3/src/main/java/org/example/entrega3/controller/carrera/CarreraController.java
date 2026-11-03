package org.example.entrega3.controller.carrera;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Request.CarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteCarreraDTO;
import org.example.entrega3.DTOs.Request.EstudianteSearchDTO;
import org.example.entrega3.DTOs.Response.*;
import org.example.entrega3.services.CarreraServices;
import org.example.entrega3.services.EstudianteCarreraService;
import org.example.entrega3.services.EstudianteServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraServices carreraServices;
    private final EstudianteCarreraService estudiantecarreraServices;

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
    public ResponseEntity<CarreraResponseDTO> update(@PathVariable Long id, @RequestBody @Valid CarreraDTO request) {
        CarreraResponseDTO carreraActualizada = this.carreraServices.update(id, request);
        return ResponseEntity.ok(carreraActualizada);  // ← 200 OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CarreraResponseDTO> delete(@PathVariable Long id) {
        CarreraResponseDTO carreraEliminada = this.carreraServices.delete(id);
        return ResponseEntity.ok(carreraEliminada);  // ← 200 OK
    }

    //f-REcuperar carreras con estudiantes incriptos y ordernar por cantdiad de inscriptos
    @GetMapping("/inscriptos")
    public ResponseEntity<List<CarreraInscriptosResponseDTO>> getCarreraConCantInscriptos() {
        List<CarreraInscriptosResponseDTO> carreras = carreraServices.getCarreraConCantInscriptos();
        return ResponseEntity.ok(carreras);  // ← 200 OK
    }
    //g- recuperar los estudiantes de una determinada carrera, filtrado por ciudad de residencia.
    @GetMapping("/{idCarrera}/estudiantes")
    public ResponseEntity<List<EstudianteResponseDTO>> getEstudiantesByCiudad(
            @PathVariable Long idCarrera,
            EstudianteSearchDTO request) throws Exception {
        List<EstudianteResponseDTO> estudiantes = carreraServices.getEstudianteByCiudad(idCarrera, request);
        return ResponseEntity.ok(estudiantes);  // ← 200 OK
    }
    //h //generar un reporte de las carreras, que para cada carrera incluya información de los
    //    //inscriptos y egresados por año. Se deben ordenar las carreras alfabéticamente, y
    //    //presentar los años de manera cronológica.
    @GetMapping("/reporte")
    public ResponseEntity<List<ReporteResponseDTO>> getReporteGeneral(){
        List<ReporteResponseDTO> carreras = estudiantecarreraServices.getReporte();
        return ResponseEntity.ok(carreras);
    }
}
