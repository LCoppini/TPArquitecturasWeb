package Repository;

import DTO.EstudianteDTO;
import Entity.Estudiante;

import java.util.List;

public interface Estudianteinter {
    List<EstudianteDTO> getEstudiantesInOrder();
    void insertEstudiante(Estudiante estudiante);
    void matricularEstudiante(Long idEstudiante, Long idCarrera);
    EstudianteDTO getEstudiantePorNumLibreta(Long numLibreta);
    List<EstudianteDTO> getEstudiantesPorGenero(String generoSolicitado);
    List<EstudianteDTO> getEstudiantesPorCarreraYCiudad(Long idCarrera, String ciudad);
}