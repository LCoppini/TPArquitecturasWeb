package Entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;


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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EstudianteCPK that = (EstudianteCPK) o;
        return Objects.equals(idEstudiante, that.idEstudiante) && Objects.equals(idCarrera, that.idCarrera);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEstudiante, idCarrera);
    }
}
