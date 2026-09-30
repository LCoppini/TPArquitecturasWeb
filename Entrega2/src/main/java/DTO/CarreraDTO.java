package DTO;

public class CarreraDTO {

    private Long idCarrera;
    private String nombreCarrera;
    private int duracion;
    private long cantidadInscriptos;

    public CarreraDTO(Long idCarrera, String nombreCarrera, int duracion, long cantidadInscriptos) {
        this.idCarrera = idCarrera;
        this.nombreCarrera = nombreCarrera;
        this.duracion = duracion;
        this.cantidadInscriptos = cantidadInscriptos;
    }

    public Long getIdCarrera() { return idCarrera; }
    public String getNombreCarrera() { return nombreCarrera; }
    public int getDuracion() { return duracion; }
    public long getCantidadInscriptos() { return cantidadInscriptos; }

    @Override
    public String toString() {
        return nombreCarrera + " (" + duracion + " años) - " + cantidadInscriptos + " inscriptos";
    }
}