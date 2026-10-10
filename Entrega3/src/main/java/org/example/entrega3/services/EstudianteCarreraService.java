package org.example.entrega3.services;


import lombok.RequiredArgsConstructor;
import org.example.entrega3.DTOs.Response.CarrreraInscriptosResponseDTO;
import org.example.entrega3.DTOs.Response.ReporteResponseDTO;
import org.example.entrega3.repository.EstudianteCarreraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("EstudianteCarrera")
@RequiredArgsConstructor
@Transactional
public class EstudianteCarreraService {

    private final EstudianteCarreraRepository estudianteCarreraRepository;

    //CRUD



    //f-REcuperar carreras con estudiantes incriptos y ordernar por cantdiad de inscriptos
    @Transactional(readOnly = true)
    public List<CarrreraInscriptosResponseDTO> getCarrreraInscriptos() {
        return estudianteCarreraRepository.carreraPorEstudinateInscriptos();
    }

    //h
    //generar un reporte de las carreras, que para cada carrera incluya información de los
    //inscriptos y egresados por año. Se deben ordenar las carreras alfabéticamente, y
    //presentar los años de manera cronológica.
    @Transactional(readOnly = true)
    public List<ReporteResponseDTO> getReporte() {
        return estudianteCarreraRepository.reporteCarreras().stream()
                .map(p-> new ReporteResponseDTO(
                        p.getNombreCarrera(),
                        p.getAnio(),
                        p.getCantidadDeInscriptos(),
                        p.getCantidadDeEgresados()))
                .toList();
    }

}
