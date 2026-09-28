package DTO;

public class ReporteCarreraDTO {

    private String nombreCarrera;
    private int anio;
    private long cantInscriptos;
    private long cantEgresados;

    public ReporteCarreraDTO(String nombreCarrera, int anio, long cantInscriptos, long cantEgresados) {
        this.nombreCarrera = nombreCarrera;
        this.anio = anio;
        this.cantInscriptos = cantInscriptos;
        this.cantEgresados = cantEgresados;
    }

    public String getNombreCarrera() {
        return nombreCarrera;
    }

    public int getAnio() {
        return anio;
    }

    public long getCantInscriptos() {
        return cantInscriptos;
    }

    public long getCantEgresados() {
        return cantEgresados;
    }
}
