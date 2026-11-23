package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.config.FilaVirtualProperties;
import com.ticketya.entrada.dto.request.ActivarFilaRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import com.ticketya.entrada.service.CompraService;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilaVirtualServiceTest {

    @Mock
    private FilaEventoRepository filaEventoRepository;

    @Mock
    private IngresoFilaRepository ingresoFilaRepository;

    @Mock
    private TandaFilaRepository tandaFilaRepository;

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private EventoClient eventoClient;

    @Mock
    private SectorClient sectorClient;

    @Mock
    private CompraService compraService;

    private FilaVirtualService service;

    @BeforeEach
    void setUp() {
        service = new FilaVirtualService(
                filaEventoRepository,
                ingresoFilaRepository,
                tandaFilaRepository,
                entradaRepository,
                eventoClient,
                sectorClient,
                compraService
        );
    }

    @Test
    void activacionLimitaDefaultsAlInventarioYNoAbreTandaVacia() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, 5, null, null, "NO_NUMERADO"));
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.empty());
        when(entradaRepository.contarEntradasActivasDeEvento(eq(20L), any(Instant.class))).thenReturn(0L);
        when(filaEventoRepository.saveAndFlush(any(FilaEvento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.empty());
        when(ingresoFilaRepository.existsByIdEventoAndEstado(20L, "EN_ESPERA")).thenReturn(false);

        var respuesta = service.activar(20L, 10L, null);

        assertEquals(5, respuesta.maxPersonas());
        assertEquals(5, respuesta.cupoEntradas());
        assertEquals("ACTIVA", respuesta.estado());
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void activarFilaActivaConParametrosDistintosResponde409YConservaLaConfiguracion() {
        FilaEvento fila = filaActiva();
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, 10, null, null, "NO_NUMERADO"));
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 10L, new ActivarFilaRequest(3, 4)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(2, fila.getMaxPersonas());
        assertEquals(4, fila.getCupoEntradas());
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void activarFilaActivaConLosMismosParametrosEsIdempotente() {
        FilaEvento fila = filaActiva();
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, 10, null, null, "NO_NUMERADO"));
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.of(fila));

        var respuesta = service.activar(20L, 10L, new ActivarFilaRequest(2, 4));

        assertEquals("ACTIVA", respuesta.estado());
        assertEquals(2, respuesta.maxPersonas());
        assertEquals(4, respuesta.cupoEntradas());
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void primerIngresoAbreTandaCuandoLaFilaActivaNoTieneUnaEnCurso() {
        FilaEvento fila = filaActiva();
        AtomicReference<IngresoFila> ingresoGuardado = new AtomicReference<>();
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(ingresoFilaRepository.buscarVigentesConBloqueo(20L, 10L)).thenReturn(List.of());
        when(ingresoFilaRepository.saveAndFlush(any(IngresoFila.class))).thenAnswer(invocacion -> {
            IngresoFila ingreso = invocacion.getArgument(0);
            ingreso.setId(100L);
            ingresoGuardado.set(ingreso);
            return ingreso;
        });
        when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.empty());
        when(ingresoFilaRepository.existsByIdEventoAndEstado(20L, "EN_ESPERA")).thenReturn(true);
        when(ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(
                eq(20L), eq("EN_ESPERA"), any(Pageable.class)
        ))
                .thenAnswer(invocacion -> {
                    IngresoFila ingreso = ingresoGuardado.get();
                    return ingreso != null && "EN_ESPERA".equals(ingreso.getEstado())
                            ? List.of(ingreso)
                            : List.of();
                });
        when(entradaRepository.contarEntradasActivasDeEvento(eq(20L), any(Instant.class))).thenReturn(0L);
        when(tandaFilaRepository.findFirstByIdEventoOrderByNumeroDesc(20L)).thenReturn(Optional.empty());
        when(tandaFilaRepository.saveAndFlush(any(TandaFila.class))).thenAnswer(invocacion -> {
            TandaFila tanda = invocacion.getArgument(0);
            tanda.setId(200L);
            return tanda;
        });

        var ingreso = service.unirse(20L, 10L);

        assertEquals("HABILITADO", ingreso.estado());
        assertEquals(200L, ingreso.idTanda());
        assertEquals(1L, ingreso.secuencia());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(ingresoFilaRepository).findByIdEventoAndEstadoOrderBySecuenciaAsc(
                eq(20L), eq("EN_ESPERA"), pageable.capture()
        );
        assertEquals(2, pageable.getValue().getPageSize());
    }

    @Test
    void nuevoIngresoEsperaLaTandaSiguienteCuandoYaHayUnaAbierta() {
        FilaEvento fila = filaActiva();
        TandaFila tanda = new TandaFila();
        tanda.setId(200L);
        tanda.setIdEvento(20L);
        tanda.setEstado("ABIERTA");
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(ingresoFilaRepository.buscarVigentesConBloqueo(20L, 10L)).thenReturn(List.of());
        when(ingresoFilaRepository.saveAndFlush(any(IngresoFila.class))).thenAnswer(invocacion -> {
            IngresoFila ingreso = invocacion.getArgument(0);
            ingreso.setId(100L);
            return ingreso;
        });
        when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.of(tanda));

        var ingreso = service.unirse(20L, 10L);

        assertEquals("EN_ESPERA", ingreso.estado());
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void filaNoPasaAAgotadaMientrasHayComprasEnCurso() {
        FilaEvento fila = filaActiva();
        fila.setCapacidadTotal(1);
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(ingresoFilaRepository.buscarVigentesConBloqueo(20L, 10L)).thenReturn(List.of());
        when(ingresoFilaRepository.saveAndFlush(any(IngresoFila.class))).thenAnswer(invocacion -> {
            IngresoFila ingreso = invocacion.getArgument(0);
            ingreso.setId(100L);
            return ingreso;
        });
        when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.empty());
        when(ingresoFilaRepository.existsByIdEventoAndEstado(20L, "EN_ESPERA")).thenReturn(true);
        when(entradaRepository.contarEntradasActivasDeEvento(eq(20L), any(Instant.class))).thenReturn(1L);
        when(compraService.hayComprasEnCurso(20L)).thenReturn(true);

        service.unirse(20L, 10L);

        assertEquals("ACTIVA", fila.getEstado());
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void reanudarFilaConCompraEnCursoNoLaMarcaAgotada() {
        FilaEvento fila = filaActiva();
        fila.setEstado("PAUSADA");
        fila.setCapacidadTotal(1);
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.empty());
        when(entradaRepository.contarEntradasActivasDeEvento(eq(20L), any(Instant.class))).thenReturn(1L);
        when(compraService.hayComprasEnCurso(20L)).thenReturn(true);

        var respuesta = service.administrar(20L, 10L, "reanudar");

        assertEquals("ACTIVA", respuesta.estado());
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void organizadorPropietarioPuedeAdministrarLaFila() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(filaActiva()));

        var respuesta = service.administrar(20L, 10L, "pausa");

        assertEquals("PAUSADA", respuesta.estado());
    }

    @Test
    void organizadorNoPropietarioRecibe403SinMutaciones() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 99L, null));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(filaEventoRepository, never()).bloquearEvento(20L);
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void eventoInexistenteRecibe404SinMutaciones() {
        when(eventoClient.obtenerPorId(20L)).thenThrow(mock(FeignException.NotFound.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 10L, null));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(filaEventoRepository, never()).bloquearEvento(20L);
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void eventoNoDisponibleRecibe503SinMutaciones() {
        when(eventoClient.obtenerPorId(20L)).thenThrow(mock(FeignException.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 10L, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(filaEventoRepository, never()).bloquearEvento(20L);
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void fallaDeUbicacionNoCreaFilaNiTanda() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(sectorClient.obtenerPorId(30L)).thenThrow(mock(FeignException.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 10L, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(filaEventoRepository, never()).bloquearEvento(20L);
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void pausarFilaYaPausadaEsIdempotente() {
        FilaEvento fila = filaActiva();
        fila.setEstado("PAUSADA");
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));

        var respuesta = service.administrar(20L, 10L, "pausa");

        assertEquals("PAUSADA", respuesta.estado());
        assertEquals("PAUSADA", fila.getEstado());
    }

    @Test
    void cerrarFilaYaCerradaEsIdempotente() {
        FilaEvento fila = filaActiva();
        fila.setEstado("CERRADA");
        fila.setCausaCierre("MANUAL");
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));

        var respuesta = service.administrar(20L, 10L, "cierre");

        assertEquals("CERRADA", respuesta.estado());
        assertEquals("MANUAL", fila.getCausaCierre());
    }

    @Test
    void reanudarFilaCerradaResponde409() {
        FilaEvento fila = filaActiva();
        fila.setEstado("CERRADA");
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.administrar(20L, 10L, "reanudar"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("CERRADA", fila.getEstado());
    }

    @Test
    void pausarFilaAgotadaResponde409() {
        FilaEvento fila = filaActiva();
        fila.setEstado("AGOTADA");
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.administrar(20L, 10L, "pausa"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("AGOTADA", fila.getEstado());
    }

    @Test
    void operacionDeEstadoNoListadaResponde409() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(filaActiva()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.administrar(20L, 10L, "operacion-inventada"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void administrarSinFilaPersistidaResponde404() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.administrar(20L, 10L, "pausa"));
    }

    @Test
    void consultarMiEstadoSinFilaPersistidaResponde404() {
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.consultarMiEstado(20L, 10L));
    }

    @Test
    void unirseSinFilaPersistidaResponde404() {
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.unirse(20L, 10L));
    }

    @Test
    void unirseEnFilaCerradaResponde409() {
        FilaEvento fila = filaActiva();
        fila.setEstado("CERRADA");
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.unirse(20L, 10L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVA", "PAUSADA", "AGOTADA"})
    void unirseSeAceptaEnTodosLosEstadosQueNoSonCerrado(String estado) {
        FilaEvento fila = filaActiva();
        fila.setEstado(estado);
        when(filaEventoRepository.buscarPorEventoConBloqueo(20L)).thenReturn(Optional.of(fila));
        when(ingresoFilaRepository.buscarVigentesConBloqueo(20L, 10L)).thenReturn(List.of());
        when(ingresoFilaRepository.saveAndFlush(any(IngresoFila.class))).thenAnswer(invocacion -> {
            IngresoFila ingreso = invocacion.getArgument(0);
            ingreso.setId(100L);
            return ingreso;
        });
        lenient().when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.empty());
        lenient().when(ingresoFilaRepository.existsByIdEventoAndEstado(20L, "EN_ESPERA")).thenReturn(false);

        var ingreso = service.unirse(20L, 10L);

        assertEquals("EN_ESPERA", ingreso.estado());
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void activacionConDisponibilidadCeroResponde409() {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, 0, null, null, "NO_NUMERADO"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 10L, null));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(filaEventoRepository, never()).bloquearEvento(20L);
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void activacionConValoresOmitidosUsaLosDefaultsSobreLaDisponibilidad() {
        stubActivarSinParametros(10);
        stubGuardarFilaSinHabilitados();

        var respuesta = service.activar(20L, 10L, new ActivarFilaRequest(null, null));

        assertEquals(10, respuesta.maxPersonas());
        assertEquals(10, respuesta.cupoEntradas());
    }

    @Test
    void activacionConSoloMaxPersonasCalculaElCupoPorDefecto() {
        stubActivarSinParametros(20);
        stubGuardarFilaSinHabilitados();

        var respuesta = service.activar(20L, 10L, new ActivarFilaRequest(3, null));

        assertEquals(3, respuesta.maxPersonas());
        assertEquals(6, respuesta.cupoEntradas());
    }

    @Test
    void activacionConSoloCupoConservaElMaxPorDefecto() {
        stubActivarSinParametros(5);
        stubGuardarFilaSinHabilitados();

        var respuesta = service.activar(20L, 10L, new ActivarFilaRequest(null, 5));

        assertEquals(5, respuesta.maxPersonas());
        assertEquals(5, respuesta.cupoEntradas());
    }

    @Test
    void activacionAceptaElLimiteExactoDeSeisEntradasPorPersona() {
        stubActivarSinParametros(30);
        stubGuardarFilaSinHabilitados();

        var respuesta = service.activar(20L, 10L, new ActivarFilaRequest(2, 12));

        assertEquals(12, respuesta.cupoEntradas());
    }

    @Test
    void activacionAceptaCupoIgualAMaxPersonas() {
        stubActivarSinParametros(10);
        stubGuardarFilaSinHabilitados();

        var respuesta = service.activar(20L, 10L, new ActivarFilaRequest(2, 2));

        assertEquals(2, respuesta.cupoEntradas());
    }

    @Test
    void valorExplicitoQueExcedeLaDisponibilidadResponde400() {
        stubActivarSinParametros(10);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.activar(20L, 10L, new ActivarFilaRequest(2, 11)));

        assertEquals("La configuración de fila excede sus límites o la disponibilidad", error.getMessage());
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void cupoPorDebajoDeMaxPersonasResponde400() {
        stubActivarSinParametros(10);

        assertThrows(IllegalArgumentException.class,
                () -> service.activar(20L, 10L, new ActivarFilaRequest(3, 2)));
    }

    @Test
    void maxPersonasCeroResponde400() {
        stubActivarSinParametros(10);

        assertThrows(IllegalArgumentException.class,
                () -> service.activar(20L, 10L, new ActivarFilaRequest(0, 4)));
    }

    @Test
    void consultasRemotasOcurrenAntesDeTomarElLockDelEvento() {
        stubActivarSinParametros(10);
        stubGuardarFilaSinHabilitados();

        service.activar(20L, 10L, null);

        InOrder orden = inOrder(eventoClient, sectorClient, filaEventoRepository);
        orden.verify(eventoClient).obtenerPorId(20L);
        orden.verify(sectorClient).obtenerPorId(30L);
        orden.verify(filaEventoRepository).bloquearEvento(20L);
        verify(eventoClient, times(1)).obtenerPorId(20L);
        verify(sectorClient, times(1)).obtenerPorId(30L);
    }

    @Test
    void falloFeignEnAdministrarNoModificaLaFilaNiAbreTanda() {
        FilaEvento fila = filaActiva();
        when(eventoClient.obtenerPorId(20L)).thenThrow(mock(FeignException.class));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.administrar(20L, 10L, "pausa"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        assertEquals("ACTIVA", fila.getEstado());
        verify(filaEventoRepository, never()).bloquearEventoCompartido(20L);
        verify(tandaFilaRepository, never()).saveAndFlush(any(TandaFila.class));
    }

    @Test
    void flagDeshabilitadoImpideActivarSinLlamadasRemotasNiMutaciones() {
        FilaVirtualProperties deshabilitado = new FilaVirtualProperties();
        deshabilitado.setEnabled(false);
        service.setFilaVirtualProperties(deshabilitado);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.activar(20L, 10L, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(eventoClient, never()).obtenerPorId(20L);
        verify(filaEventoRepository, never()).bloquearEvento(20L);
        verify(filaEventoRepository, never()).saveAndFlush(any(FilaEvento.class));
    }

    @Test
    void flagDeshabilitadoImpideUnirseSinTomarLocksNiMutaciones() {
        FilaVirtualProperties deshabilitado = new FilaVirtualProperties();
        deshabilitado.setEnabled(false);
        service.setFilaVirtualProperties(deshabilitado);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.unirse(20L, 10L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(filaEventoRepository, never()).bloquearEventoCompartido(20L);
        verify(ingresoFilaRepository, never()).saveAndFlush(any(IngresoFila.class));
    }

    private void stubActivarSinParametros(int capacidad) {
        when(eventoClient.obtenerPorId(20L)).thenReturn(eventoPublicado(10L, 30L));
        when(sectorClient.obtenerPorId(30L))
                .thenReturn(new SectorResponse(30L, capacidad, null, null, "NO_NUMERADO"));
        when(filaEventoRepository.findById(20L)).thenReturn(Optional.empty());
        when(entradaRepository.contarEntradasActivasDeEvento(eq(20L), any(Instant.class))).thenReturn(0L);
    }

    private void stubGuardarFilaSinHabilitados() {
        when(filaEventoRepository.saveAndFlush(any(FilaEvento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(20L)).thenReturn(Optional.empty());
        when(ingresoFilaRepository.existsByIdEventoAndEstado(20L, "EN_ESPERA")).thenReturn(true);
        when(tandaFilaRepository.findFirstByIdEventoOrderByNumeroDesc(20L)).thenReturn(Optional.empty());
        when(tandaFilaRepository.saveAndFlush(any(TandaFila.class))).thenAnswer(invocacion -> {
            TandaFila tanda = invocacion.getArgument(0);
            tanda.setId(200L);
            return tanda;
        });
        when(ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(
                eq(20L), eq("EN_ESPERA"), any(Pageable.class)
        )).thenReturn(List.of());
    }

    private EventoResponse eventoPublicado(Long idOrganizador, Long idSector) {
        return new EventoResponse(
                20L,
                null,
                idOrganizador,
                "Evento de prueba",
                null,
                Instant.now().plusSeconds(86_400),
                "PUBLICADO",
                "SHOW",
                List.of(),
                List.of(new EventoSectorResponse(idSector, new BigDecimal("100.00")))
        );
    }

    private FilaEvento filaActiva() {
        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(20L);
        fila.setEstado("ACTIVA");
        fila.setMaxPersonas(2);
        fila.setCupoEntradas(4);
        fila.setCapacidadTotal(10);
        fila.setSiguienteSecuencia(1L);
        return fila;
    }
}
