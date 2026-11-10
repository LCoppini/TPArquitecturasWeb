package com.ticketya.ubicacion.service.impl;

import com.ticketya.ubicacion.dto.request.ActualizarLugarRequest;
import com.ticketya.ubicacion.dto.request.LugarRequest;
import com.ticketya.ubicacion.dto.request.SectorRequest;
import com.ticketya.ubicacion.dto.response.LugarResponse;
import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.entity.Lugar;
import com.ticketya.ubicacion.entity.Sector;
import com.ticketya.ubicacion.entity.TipoSector;
import com.ticketya.ubicacion.exception.RecursoNoEncontradoException;
import com.ticketya.ubicacion.exception.ReglaNegocioException;
import com.ticketya.ubicacion.mapper.UbicacionMapper;
import com.ticketya.ubicacion.repository.LugarRepository;
import com.ticketya.ubicacion.repository.SectorRepository;
import com.ticketya.ubicacion.service.LugarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LugarServiceImpl implements LugarService {

    private final LugarRepository lugarRepository;
    private final SectorRepository sectorRepository;
    private final UbicacionMapper mapper;

    public LugarServiceImpl(
            LugarRepository lugarRepository,
            SectorRepository sectorRepository,
            UbicacionMapper mapper
    ) {
        this.lugarRepository = lugarRepository;
        this.sectorRepository = sectorRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public LugarResponse crear(LugarRequest request) {
        validarSectores(request.sectores());

        Lugar lugar = new Lugar(
                request.nombre().trim(),
                request.direccion().trim(),
                request.ciudad().trim(),
                request.latitud(),
                request.longitud()
        );
        request.sectores().forEach(sector -> lugar.agregarSector(mapper.aEntidad(sector)));
        return mapper.aResponse(lugarRepository.save(lugar));
    }

    @Override
    public LugarResponse obtenerPorId(Long id) {
        return mapper.aResponse(buscarLugar(id));
    }

    @Override
    public List<LugarResponse> listar(String ciudad, List<Long> ids) {
        List<Lugar> lugares;
        if (ids != null && !ids.isEmpty()) {
            lugares = lugarRepository.findAllById(ids);
        } else if (ciudad != null && !ciudad.isBlank()) {
            lugares = lugarRepository.findByCiudadIgnoreCaseOrderByIdAsc(ciudad.trim());
        } else {
            lugares = lugarRepository.findAllByOrderByIdAsc();
        }
        return lugares.stream()
                .filter(lugar -> ciudad == null || ciudad.isBlank() || lugar.getCiudad().equalsIgnoreCase(ciudad.trim()))
                .map(mapper::aResponse)
                .toList();
    }

    @Override
    @Transactional
    public LugarResponse actualizar(Long id, ActualizarLugarRequest request) {
        Lugar lugar = buscarLugar(id);
        lugar.setNombre(request.nombre().trim());
        lugar.setDireccion(request.direccion().trim());
        lugar.setCiudad(request.ciudad().trim());
        lugar.setLatitud(request.latitud());
        lugar.setLongitud(request.longitud());
        return mapper.aResponse(lugarRepository.save(lugar));
    }

    @Override
    @Transactional
    public SectorResponse agregarSector(Long idLugar, SectorRequest request) {
        Lugar lugar = buscarLugar(idLugar);
        validarSectores(List.of(request));
        if (sectorRepository.existsByLugarIdAndNombreIgnoreCase(idLugar, request.nombre().trim())) {
            throw new ReglaNegocioException(
                    "El lugar " + idLugar + " ya tiene un sector llamado '" + request.nombre().trim() + "'"
            );
        }
        Sector sector = mapper.aEntidad(request);
        lugar.agregarSector(sector);
        return mapper.aResponse(sectorRepository.save(sector));
    }

    @Override
    public List<SectorResponse> listarSectores(Long idLugar) {
        buscarLugar(idLugar);
        return sectorRepository.findByLugarIdOrderByIdAsc(idLugar).stream()
                .map(mapper::aResponse)
                .toList();
    }

    private Lugar buscarLugar(Long id) {
        return lugarRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lugar con id " + id));
    }

    /**
     * Reglas que Bean Validation no cubre: coherencia entre tipo y datos de capacidad, y nombres
     * repetidos dentro de la misma solicitud. Acumula todos los errores para devolverlos juntos (400).
     */
    private void validarSectores(List<SectorRequest> sectores) {
        List<String> errores = new ArrayList<>();
        Set<String> nombres = new HashSet<>();

        for (int i = 0; i < sectores.size(); i++) {
            SectorRequest sector = sectores.get(i);
            String prefijo = "sectores[" + i + "]: ";

            if (!nombres.add(sector.nombre().trim().toLowerCase(Locale.ROOT))) {
                errores.add(prefijo + "el nombre '" + sector.nombre().trim() + "' esta repetido en la solicitud");
            }

            if (TipoSector.NUMERADO.equals(sector.tipo())) {
                if (sector.filas() == null || sector.asientosPorFila() == null) {
                    errores.add(prefijo + "un sector NUMERADO requiere filas y asientosPorFila mayores a cero");
                } else if (sector.capacidadMax() != null
                        && sector.capacidadMax() != sector.filas() * sector.asientosPorFila()) {
                    errores.add(prefijo + "la capacidad de un sector NUMERADO es filas por asientosPorFila ("
                            + sector.filas() * sector.asientosPorFila() + "); no se puede informar otra");
                }
            } else if (TipoSector.NO_NUMERADO.equals(sector.tipo())) {
                if (sector.capacidadMax() == null) {
                    errores.add(prefijo + "un sector NO_NUMERADO requiere capacidadMax mayor a cero");
                }
                if (sector.filas() != null || sector.asientosPorFila() != null) {
                    errores.add(prefijo + "un sector NO_NUMERADO no admite filas ni asientosPorFila");
                }
            }
        }

        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }
    }
}
