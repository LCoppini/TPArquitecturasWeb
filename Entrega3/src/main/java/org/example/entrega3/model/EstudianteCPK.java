package org.example.entrega3.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;


@Embeddable
public class EstudianteCPK implements Serializable {

    @Column
    private Long idEstudiante;

    @Column
    private Long idCarrera;

    public EstudianteCPK() {
    }

    public EstudianteCPK(Long idEstudiante, Long idCarrera) {
        this.idEstudiante = idEstudiante;
        this.idCarrera = idCarrera;
    }

    public Long getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(Long idCarrera) {
        this.idCarrera = idCarrera;
    }

    public Long getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(Long idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

}
