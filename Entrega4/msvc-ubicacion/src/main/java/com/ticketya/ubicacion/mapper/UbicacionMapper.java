package com.ticketya.ubicacion.mapper;

import com.ticketya.ubicacion.dto.request.SectorRequest;
import com.ticketya.ubicacion.dto.response.LugarResponse;
import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.entity.Lugar;
import com.ticketya.ubicacion.entity.Sector;
import com.ticketya.ubicacion.entity.TipoSector;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;

@Component
public class UbicacionMapper {

    public LugarResponse aResponse(Lugar lugar) {
        return new LugarResponse(
                lugar.getId(),
                lugar.getNombre(),
                lugar.getDireccion(),
                lugar.getCiudad(),
                lugar.getLatitud() + "," + lugar.getLongitud(),
                lugar.getLatitud(),
                lugar.getLongitud(),
                lugar.getSectores().stream()
                        .map(this::aResponse)
                        .sorted(Comparator.comparing(SectorResponse::idSector, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList()
        );
    }

    public SectorResponse aResponse(Sector sector) {
        return new SectorResponse(
                sector.getId(),
                sector.getIdLugar(),
                sector.getNombre(),
                sector.getTipo(),
                sector.getFilas(),
                sector.getAsientosPorFila(),
                sector.getCapacidadMax()
        );
    }

    /** Convierte un request ya validado en entidad; calcula la capacidad de los sectores numerados. */
    public Sector aEntidad(SectorRequest request) {
        Sector sector = new Sector();
        sector.setNombre(request.nombre().trim());
        sector.setTipo(request.tipo());
        if (TipoSector.NUMERADO.equals(request.tipo())) {
            sector.setFilas(request.filas());
            sector.setAsientosPorFila(request.asientosPorFila());
            sector.setCapacidadMax(Objects.requireNonNull(request.filas()) * Objects.requireNonNull(request.asientosPorFila()));
        } else {
            sector.setCapacidadMax(request.capacidadMax());
        }
        return sector;
    }
}
