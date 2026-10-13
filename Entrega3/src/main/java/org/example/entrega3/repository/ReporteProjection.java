package org.example.entrega3.repository;

public interface ReporteProjection {
    Long getIdCarrera();
    String getNombreCarrera();
    Integer getAnio();
    Long getCantidadDeInscriptos();
    Long getCantidadDeEgresados();
}
