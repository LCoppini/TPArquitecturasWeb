package Repository;

import DTO.CarreraDTO;
import Entity.Carrera;

import java.util.List;

public interface CarreraInter {
    List<CarreraDTO> getCarrerasPorCantidadInscriptos();
}
