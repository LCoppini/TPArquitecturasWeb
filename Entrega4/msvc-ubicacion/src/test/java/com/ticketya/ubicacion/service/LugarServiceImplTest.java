package com.ticketya.ubicacion.service;

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
import com.ticketya.ubicacion.service.impl.LugarServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LugarServiceImplTest {

    @Mock
    private LugarRepository lugarRepository;

    @Mock
    private SectorRepository sectorRepository;

    private LugarServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LugarServiceImpl(lugarRepository, sectorRepository, new UbicacionMapper());
    }

    private static SectorRequest numerado(String nombre, Integer filas, Integer asientos) {
        return new SectorRequest(nombre, TipoSector.NUMERADO, filas, asientos, null);
    }

    private static SectorRequest zona(String nombre, Integer capacidad) {
        return new SectorRequest(nombre, TipoSector.NO_NUMERADO, null, null, capacidad);
    }

    private static LugarRequest request(SectorRequest... sectores) {
        return new LugarRequest("Estadio Central", "Av. Siempre Viva 123", "Tandil", -37.32, -59.13, List.of(sectores));
    }

    private void guardarAsignandoIds() {
        AtomicLong ids = new AtomicLong(10);
        when(lugarRepository.save(any(Lugar.class))).thenAnswer(invocacion -> {
            Lugar lugar = invocacion.getArgument(0);
            lugar.setId(1L);
            lugar.getSectores().forEach(sector -> sector.setId(ids.incrementAndGet()));
            return lugar;
        });
    }

    private Lugar lugarExistente(Long id) {
        Lugar lugar = new Lugar("Teatro", "Calle 1", "Tandil", -37.3, -59.1);
        lugar.setId(id);
        return lugar;
    }

    @Test
    void crearCalculaCapacidadDeSectorNumeradoComoFilasPorAsientos() {
        guardarAsignandoIds();

        LugarResponse respuesta = service.crear(request(numerado("Platea", 10, 20)));

        SectorResponse sector = respuesta.sectores().getFirst();
        assertEquals(200, sector.capacidadMax());
        assertEquals(10, sector.filas());
        assertEquals(20, sector.asientosPorFila());
        assertEquals(TipoSector.NUMERADO, sector.tipo());
        assertEquals(1L, sector.idLugar());
    }

    @Test
    void crearConservaCapacidadDeSectorNoNumeradoYNoGuardaFilas() {
        guardarAsignandoIds();

        LugarResponse respuesta = service.crear(request(zona("Campo", 5000)));

        SectorResponse sector = respuesta.sectores().getFirst();
        assertEquals(5000, sector.capacidadMax());
        assertNull(sector.filas());
        assertNull(sector.asientosPorFila());
    }

    @Test
    void crearArmaUbicacionConLatitudYLongitudYDevuelveCiudad() {
        guardarAsignandoIds();

        LugarResponse respuesta = service.crear(request(zona("Campo", 100)));

        assertEquals("-37.32,-59.13", respuesta.ubicacion());
        assertEquals("Tandil", respuesta.ciudad());
        assertEquals(-37.32, respuesta.latitud());
        assertEquals(-59.13, respuesta.longitud());
    }

    @Test
    void crearRechazaSectorNumeradoSinFilasYAcumulaTodosLosErrores() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.crear(request(
                numerado("Platea", null, 20),
                zona("Campo", null)
        )));

        assertTrue(error.getMessage().contains("sectores[0]"));
        assertTrue(error.getMessage().contains("NUMERADO requiere filas"));
        assertTrue(error.getMessage().contains("sectores[1]"));
        assertTrue(error.getMessage().contains("NO_NUMERADO requiere capacidadMax"));
        verify(lugarRepository, never()).save(any());
    }

    @Test
    void crearRechazaSectorNoNumeradoConFilas() {
        SectorRequest invalido = new SectorRequest("Campo", TipoSector.NO_NUMERADO, 3, 4, 100);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crear(request(invalido)));

        assertTrue(error.getMessage().contains("no admite filas"));
    }

    @Test
    void crearRechazaCapacidadIncoherenteEnSectorNumerado() {
        SectorRequest invalido = new SectorRequest("Platea", TipoSector.NUMERADO, 10, 20, 150);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crear(request(invalido)));

        assertTrue(error.getMessage().contains("(200)"));
    }

    @Test
    void crearRechazaNombresDeSectorRepetidosEnLaSolicitud() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.crear(request(zona("Campo", 100), zona("campo", 200))));

        assertTrue(error.getMessage().contains("repetido"));
    }

    @Test
    void obtenerPorIdInexistenteDevuelveNoEncontrado() {
        when(lugarRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void listarPorCiudadUsaFiltroSinDistinguirMayusculas() {
        when(lugarRepository.findByCiudadIgnoreCaseOrderByIdAsc("tandil")).thenReturn(List.of(lugarExistente(1L)));

        List<LugarResponse> respuesta = service.listar("tandil", null);

        assertEquals(1, respuesta.size());
        assertEquals("Tandil", respuesta.getFirst().ciudad());
    }

    @Test
    void listarPorIdsConsultaEnBloque() {
        when(lugarRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(lugarExistente(1L), lugarExistente(2L)));

        List<LugarResponse> respuesta = service.listar(null, List.of(1L, 2L));

        assertEquals(2, respuesta.size());
    }

    @Test
    void listarSinFiltrosDevuelveTodos() {
        when(lugarRepository.findAllByOrderByIdAsc()).thenReturn(List.of(lugarExistente(1L)));

        assertEquals(1, service.listar(null, null).size());
    }

    @Test
    void actualizarModificaDatosDelLugarSinTocarSectores() {
        Lugar lugar = lugarExistente(1L);
        Sector sector = new Sector();
        sector.setId(5L);
        sector.setNombre("Campo");
        sector.setTipo(TipoSector.NO_NUMERADO);
        sector.setCapacidadMax(100);
        lugar.agregarSector(sector);
        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(lugarRepository.save(lugar)).thenReturn(lugar);

        LugarResponse respuesta = service.actualizar(1L,
                new ActualizarLugarRequest("Teatro Nuevo", "Calle 2", "Azul", -36.77, -59.85));

        assertEquals("Teatro Nuevo", respuesta.nombre());
        assertEquals("Azul", respuesta.ciudad());
        assertEquals(1, respuesta.sectores().size());
        assertEquals(5L, respuesta.sectores().getFirst().idSector());
    }

    @Test
    void agregarSectorAsociaElSectorAlLugar() {
        Lugar lugar = lugarExistente(1L);
        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(sectorRepository.existsByLugarIdAndNombreIgnoreCase(1L, "VIP")).thenReturn(false);
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocacion -> {
            Sector sector = invocacion.getArgument(0);
            sector.setId(77L);
            return sector;
        });

        SectorResponse respuesta = service.agregarSector(1L, zona("VIP", 50));

        assertEquals(77L, respuesta.idSector());
        assertEquals(1L, respuesta.idLugar());
        assertEquals(50, respuesta.capacidadMax());
    }

    @Test
    void agregarSectorConNombreExistenteDevuelveConflicto() {
        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugarExistente(1L)));
        when(sectorRepository.existsByLugarIdAndNombreIgnoreCase(1L, "VIP")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.agregarSector(1L, zona("VIP", 50)));
        verify(sectorRepository, never()).save(any());
    }

    @Test
    void agregarSectorEnLugarInexistenteDevuelveNoEncontrado() {
        when(lugarRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.agregarSector(9L, zona("VIP", 50)));
    }

    @Test
    void listarSectoresDevuelveLosDelLugar() {
        Sector sector = new Sector();
        sector.setId(3L);
        sector.setNombre("Campo");
        sector.setTipo(TipoSector.NO_NUMERADO);
        sector.setCapacidadMax(100);
        Lugar lugar = lugarExistente(1L);
        lugar.agregarSector(sector);
        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(sectorRepository.findByLugarIdOrderByIdAsc(1L)).thenReturn(List.of(sector));

        List<SectorResponse> respuesta = service.listarSectores(1L);

        assertEquals(1, respuesta.size());
        assertEquals(3L, respuesta.getFirst().idSector());
    }
}
