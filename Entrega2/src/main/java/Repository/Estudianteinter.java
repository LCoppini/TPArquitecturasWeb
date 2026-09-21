package Repository;

import Entity.Estudiante;

import java.util.List;

public interface Estudianteinter {
    public List<Estudiante> getEstudiantesInOrder();
    void insertEstudiante(Estudiante estudiante);
    void matricularEstudiante(Long idEstudiante, Long idCarrera);
    Estudiante getEstudiantePorNumLibreta(Long numLibreta);
    List<Estudiante> getEstudiantesPorGenero(String generoSolicitado);
}
