package Repository;

import Entity.Estudiante;

public interface Estudianteinter {
    void insertEstudiante(Estudiante estudiante);
    void matricularEstudiante(Long idEstudiante, Long idCarrera);
}
