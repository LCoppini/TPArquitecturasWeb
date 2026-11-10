package com.ticketya.ubicacion.service;

import com.ticketya.ubicacion.dto.response.SectorResponse;
import com.ticketya.ubicacion.entity.Lugar;
import com.ticketya.ubicacion.entity.Sector;
import com.ticketya.ubicacion.entity.TipoSector;
import com.ticketya.ubicacion.exception.RecursoNoEncontradoException;
import com.ticketya.ubicacion.mapper.UbicacionMapper;
import com.ticketya.ubicacion.repository.SectorRepository;
import com.ticketya.ubicacion.service.impl.SectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectorServiceImplTest {

    @Mock
    private SectorRepository sectorRepository;

    private SectorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SectorServiceImpl(sectorRepository, new UbicacionMapper());
    }

    @Test
    void obtenerPorIdDevuelveEstructuraConIdLugarYSinPrecio() {
        Lugar lugar = new Lugar("Teatro", "Calle 1", "Tandil", -37.3, -59.1);
        lugar.setId(4L);
        Sector sector = new Sector();
        sector.setId(31L);
        sector.setLugar(lugar);
        sector.setNombre("Platea");
        sector.setTipo(TipoSector.NUMERADO);
        sector.setFilas(5);
        sector.setAsientosPorFila(10);
        sector.setCapacidadMax(50);
        when(sectorRepository.findById(31L)).thenReturn(Optional.of(sector));

        SectorResponse respuesta = service.obtenerPorId(31L);

        assertEquals(31L, respuesta.idSector());
        assertEquals(4L, respuesta.idLugar());
        assertEquals("Platea", respuesta.nombre());
        assertEquals(TipoSector.NUMERADO, respuesta.tipo());
        assertEquals(50, respuesta.capacidadMax());
    }

    @Test
    void obtenerPorIdInexistenteDevuelveNoEncontrado() {
        when(sectorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }
}
