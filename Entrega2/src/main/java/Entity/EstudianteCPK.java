package Entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;


//Preguntar si se hace asi
@Embeddable
public class EstudianteCPK implements Serializable {
    // aca poner la relacion

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
