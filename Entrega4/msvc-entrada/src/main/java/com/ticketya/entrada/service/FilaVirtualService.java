package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.config.FilaVirtualProperties;
import com.ticketya.entrada.dto.request.ActivarFilaRequest;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.FilaEventoResponse;
import com.ticketya.entrada.dto.response.IngresoFilaResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FilaVirtualService {

    private static final Logger log = LoggerFactory.getLogger(FilaVirtualService.class);
    private static final String ACTIVA = "ACTIVA";
    private static final String PAUSADA = "PAUSADA";
    private static final String AGOTADA = "AGOTADA";
    private static final String CERRADA = "CERRADA";
    private static final String EN_ESPERA = "EN_ESPERA";
    private static final String HABILITADO = "HABILITADO";
    private static final String ABIERTA = "ABIERTA";
    private static final String CERRADA_TANDA = "CERRADA";

    private final FilaEventoRepository filaEventoRepository;
    private final IngresoFilaRepository ingresoFilaRepository;
    private final TandaFilaRepository tandaFilaRepository;
    private final EntradaRepository entradaRepository;
    private final EventoClient eventoClient;
    private final SectorClient sectorClient;
    private final CompraService compraService;

    private FilaVirtualProperties filaVirtualProperties = new FilaVirtualProperties();

    @Autowired(required = false)
    public void setFilaVirtualProperties(FilaVirtualProperties filaVirtualProperties) {
        if (filaVirtualProperties != null) {
            this.filaVirtualProperties = filaVirtualProperties;
        }
    }

    private void validarFilaHabilitada() {
        if (!filaVirtualProperties.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "La fila virtual está deshabilitada por configuración"
            );
        }
    }

    @Autowired
    public FilaVirtualService(
            FilaEventoRepository filaEventoRepository,
            IngresoFilaRepository ingresoFilaRepository,
            TandaFilaRepository tandaFilaRepository,
            EntradaRepository entradaRepository,
            EventoClient eventoClient,
            SectorClient sectorClient,
            CompraService compraService
    ) {
        this.filaEventoRepository = filaEventoRepository;
        this.ingresoFilaRepository = ingresoFilaRepository;
        this.tandaFilaRepository = tandaFilaRepository;
        this.entradaRepository = entradaRepository;
        this.eventoClient = eventoClient;
        this.sectorClient = sectorClient;
        this.compraService = compraService;
    }

    public FilaVirtualService(
            FilaEventoRepository filaEventoRepository,
            IngresoFilaRepository ingresoFilaRepository,
            TandaFilaRepository tandaFilaRepository,
            EntradaRepository entradaRepository,
            EventoClient eventoClient,
            SectorClient sectorClient
    ) {
        this(filaEventoRepository, ingresoFilaRepository, tandaFilaRepository,
                entradaRepository, eventoClient, sectorClient, null);
    }

    @Transactional
    public FilaEventoResponse activar(Long idEvento, Long idOrganizador, ActivarFilaRequest request) {
        validarFilaHabilitada();
        EventoResponse evento = obtenerEvento(idEvento);
        validarPropietario(evento, idOrganizador);
        if (!"PUBLICADO".equalsIgnoreCase(evento.estado())) {
            throw conflicto("Solo se puede activar la fila para un evento PUBLICADO");
        }
        int capacidad = obtenerCapacidadTotal(evento);
        if (capacidad <= 0) {
            throw conflicto("El evento no tiene entradas disponibles");
        }

        filaEventoRepository.bloquearEvento(idEvento);
        FilaEvento existente = filaEventoRepository.findById(idEvento).orElse(null);
        if (existente != null) {
            if (ACTIVA.equals(existente.getEstado())) {
                validarParametrosInmutables(existente, request);
                return convertirFila(existente);
            }
            throw conflicto("Ya existe una fila persistida para el evento");
        }

        Instant ahora = Instant.now();
        if (entradaRepository.contarEntradasActivasDeEvento(idEvento, ahora) > 0) {
            throw conflicto("El evento ya tiene entradas reservadas o vendidas");
        }

        int maxPersonas = request == null || request.maxPersonas() == null
                ? Math.min(20, capacidad)
                : request.maxPersonas();
        int cupoEntradas = request == null || request.cupoEntradas() == null
                ? (int) Math.min((long) maxPersonas * 2, capacidad)
                : request.cupoEntradas();
        validarConfiguracion(maxPersonas, cupoEntradas, capacidad);

        FilaEvento fila = new FilaEvento();
        fila.setIdEvento(idEvento);
        fila.setEstado(ACTIVA);
        fila.setMaxPersonas(maxPersonas);
        fila.setCupoEntradas(cupoEntradas);
        fila.setCapacidadTotal(capacidad);
        fila.setSiguienteSecuencia(1L);
        fila = filaEventoRepository.saveAndFlush(fila);
        abrirTandaSiCorresponde(fila, ahora);
        return convertirFila(fila);
    }

    @Transactional
    public IngresoFilaResponse unirse(Long idEvento, Long idUsuario) {
        validarFilaHabilitada();
        filaEventoRepository.bloquearEventoCompartido(idEvento);
        FilaEvento fila = buscarFilaConBloqueo(idEvento);
        if (CERRADA.equals(fila.getEstado())) {
            throw conflicto("La fila está cerrada y no acepta nuevos ingresos");
        }

        List<IngresoFila> membresiasVigentes = ingresoFilaRepository.buscarVigentesConBloqueo(
                idEvento, idUsuario
        );
        if (!membresiasVigentes.isEmpty()) {
            return convertirIngreso(membresiasVigentes.getFirst());
        }

        IngresoFila ingreso = new IngresoFila();
        ingreso.setIdEvento(idEvento);
        ingreso.setIdUsuario(idUsuario);
        ingreso.setSecuencia(fila.getSiguienteSecuencia());
        ingreso.setEstado(EN_ESPERA);
        ingreso.setIngresadoEn(Instant.now());
        fila.setSiguienteSecuencia(fila.getSiguienteSecuencia() + 1);
        ingreso = ingresoFilaRepository.saveAndFlush(ingreso);

        if (ACTIVA.equals(fila.getEstado())) {
            abrirTandaSiCorresponde(fila, Instant.now());
        }
        log.info("ingreso_fila idEvento={} idUsuario={} secuencia={} estado={} correlationId={}",
                idEvento, idUsuario, ingreso.getSecuencia(), ingreso.getEstado(), correlationId());
        return convertirIngreso(ingreso);
    }

    public IngresoFilaResponse consultarMiEstado(Long idEvento, Long idUsuario) {
        buscarFila(idEvento);
        IngresoFila ingreso = ingresoFilaRepository
                .findFirstByIdEventoAndIdUsuarioOrderBySecuenciaDesc(idEvento, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un ingreso para este usuario"));
        return convertirIngreso(ingreso);
    }

    @Transactional
    public FilaEventoResponse administrar(
            Long idEvento,
            Long idOrganizador,
            String operacion
    ) {
        EventoResponse evento = obtenerEvento(idEvento);
        validarPropietario(evento, idOrganizador);
        filaEventoRepository.bloquearEventoCompartido(idEvento);
        FilaEvento fila = buscarFilaConBloqueo(idEvento);
        String estadoActual = fila.getEstado();
        Instant ahora = Instant.now();

        switch (operacion) {
            case "pausa" -> {
                if (PAUSADA.equals(estadoActual)) {
                    return convertirFila(fila);
                }
                if (!ACTIVA.equals(estadoActual)) {
                    throw conflicto("La fila solo se puede pausar desde ACTIVA");
                }
                fila.setEstado(PAUSADA);
            }
            case "reanudar" -> {
                if (ACTIVA.equals(estadoActual)) {
                    return convertirFila(fila);
                }
                if (!PAUSADA.equals(estadoActual) && !AGOTADA.equals(estadoActual)) {
                    throw conflicto("La fila cerrada no se puede reanudar");
                }
                if (tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(idEvento).isPresent()) {
                    fila.setEstado(ACTIVA);
                    return convertirFila(fila);
                }
                long disponibles = entradasDisponibles(fila, ahora);
                if (disponibles <= 0) {
                    if (compraService == null || !compraService.hayComprasEnCurso(idEvento)) {
                        fila.setEstado(AGOTADA);
                    } else {
                        fila.setEstado(ACTIVA);
                    }
                    return convertirFila(fila);
                }
                fila.setEstado(ACTIVA);
                abrirTandaSiCorresponde(fila, ahora);
            }
            case "cierre" -> {
                if (CERRADA.equals(estadoActual)) {
                    return convertirFila(fila);
                }
                fila.setEstado(CERRADA);
                fila.setCausaCierre("MANUAL");
                ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(idEvento, EN_ESPERA)
                        .forEach(ingreso -> ingreso.setEstado("SALIO"));
            }
            default -> throw conflicto("Operación de fila no permitida");
        }
        return convertirFila(fila);
    }

    @Transactional
    public void cerrarTandaVencida(Long idEvento) {
        filaEventoRepository.bloquearEvento(idEvento);
        FilaEvento fila = filaEventoRepository.buscarPorEventoConBloqueo(idEvento).orElse(null);
        if (fila == null) {
            return;
        }
        TandaFila tanda = tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(idEvento).orElse(null);
        Instant ahora = Instant.now();
        if (tanda == null || tanda.getVenceEn().isAfter(ahora)) {
            return;
        }
        log.info("tanda_vencida idEvento={} idTanda={} correlationId={}",
                idEvento, tanda.getId(), correlationId());

        if (compraService != null) {
            compraService.vencerComprasTanda(idEvento, tanda.getId(), ahora);
        }
        tanda.setEstado(CERRADA_TANDA);
        tanda.setCerradaEn(ahora);
        ingresoFilaRepository.buscarHabilitadosDeTanda(idEvento, tanda.getId())
                .forEach(ingreso -> {
                    boolean rechazoCupo = compraService != null
                            && compraService.recibioRechazoDeCupo(ingreso.getIdUsuario(), tanda.getId());
                    ingreso.setEstado(rechazoCupo ? EN_ESPERA : "VENCIDO");
                    ingreso.setVenceEn(null);
                });
        if (ACTIVA.equals(fila.getEstado())) {
            abrirTandaSiCorresponde(fila, ahora);
        }
    }

    @Transactional
    public void cancelarEvento(Long idEvento) {
        log.info("cancelacion_fila_iniciada idEvento={} correlationId={}", idEvento, correlationId());
        filaEventoRepository.bloquearEvento(idEvento);
        FilaEvento fila = filaEventoRepository.buscarPorEventoConBloqueo(idEvento).orElse(null);
        if (fila == null) {
            return;
        }
        TandaFila tanda = tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(idEvento).orElse(null);
        if (compraService != null) {
            compraService.cancelarComprasPendientesEvento(idEvento);
        }
        fila.setEstado(CERRADA);
        fila.setCausaCierre("CANCELACION");
        ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(idEvento, EN_ESPERA)
                .forEach(ingreso -> ingreso.setEstado("SALIO"));
        if (tanda != null) {
            tanda.setEstado(CERRADA_TANDA);
            tanda.setCerradaEn(Instant.now());
            ingresoFilaRepository.buscarHabilitadosDeTanda(idEvento, tanda.getId())
                    .forEach(ingreso -> ingreso.setEstado("CANCELADO"));
        }
        log.info("cancelacion_fila_completada idEvento={} correlationId={}", idEvento, correlationId());
    }

    private String correlationId() {
        String correlationId = MDC.get(com.ticketya.entrada.config.CorrelationContext.MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "no-disponible" : correlationId;
    }

    private void abrirTandaSiCorresponde(FilaEvento fila, Instant ahora) {
        if (!ACTIVA.equals(fila.getEstado())
                || tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(fila.getIdEvento()).isPresent()) {
            return;
        }
        if (!ingresoFilaRepository.existsByIdEventoAndEstado(fila.getIdEvento(), EN_ESPERA)) {
            return;
        }
        long disponibles = entradasDisponibles(fila, ahora);
        if (disponibles <= 0) {
            if (compraService == null || !compraService.hayComprasEnCurso(fila.getIdEvento())) {
                fila.setEstado(AGOTADA);
            }
            return;
        }

        TandaFila anterior = tandaFilaRepository.findFirstByIdEventoOrderByNumeroDesc(fila.getIdEvento())
                .orElse(null);
        long remanente = anterior == null ? 0 : anterior.getCupoRestante();
        int cupoEfectivo = (int) Math.min((long) fila.getCupoEntradas() + remanente, disponibles);
        if (cupoEfectivo <= 0) {
            if (compraService == null || !compraService.hayComprasEnCurso(fila.getIdEvento())) {
                fila.setEstado(AGOTADA);
            }
            return;
        }

        TandaFila tanda = new TandaFila();
        tanda.setIdEvento(fila.getIdEvento());
        tanda.setNumero(anterior == null ? 1 : anterior.getNumero() + 1);
        tanda.setEstado(ABIERTA);
        tanda.setIniciadaEn(ahora);
        tanda.setVenceEn(ahora.plus(10, ChronoUnit.MINUTES));
        tanda.setCupoEfectivo(cupoEfectivo);
        tanda.setCupoRestante(cupoEfectivo);
        tanda = tandaFilaRepository.saveAndFlush(tanda);

        List<IngresoFila> esperando = ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(
                fila.getIdEvento(), EN_ESPERA, PageRequest.of(0, fila.getMaxPersonas())
        );
        int habilitados = esperando.size();
        for (int indice = 0; indice < habilitados; indice++) {
            IngresoFila ingreso = esperando.get(indice);
            ingreso.setEstado(HABILITADO);
            ingreso.setIdTanda(tanda.getId());
            ingreso.setVenceEn(tanda.getVenceEn());
        }
    }

    private long entradasDisponibles(FilaEvento fila, Instant ahora) {
        return Math.max(
                0L,
                (long) fila.getCapacidadTotal()
                        - entradaRepository.contarEntradasActivasDeEvento(fila.getIdEvento(), ahora)
        );
    }

    private int obtenerCapacidadTotal(EventoResponse evento) {
        if (evento.sectores() == null || evento.sectores().isEmpty()) {
            return 0;
        }
        long total = 0;
        for (EventoSectorResponse sectorEvento : evento.sectores()) {
            if (sectorEvento == null || sectorEvento.idSector() == null) {
                throw servicioNoDisponible("Eventos devolvió sectores incompletos");
            }
            SectorResponse sector = llamarUbicacion(sectorEvento.idSector());
            if (!sectorEvento.idSector().equals(sector.idSector())
                    || sector.capacidadMax() == null
                    || sector.capacidadMax() < 0) {
                throw servicioNoDisponible("Ubicación devolvió una capacidad inválida");
            }
            total += sector.capacidadMax();
            if (total > Integer.MAX_VALUE) {
                throw servicioNoDisponible("La capacidad del evento excede el límite admitido");
            }
        }
        return (int) total;
    }

    private EventoResponse obtenerEvento(Long idEvento) {
        try {
            EventoResponse evento = eventoClient.obtenerPorId(idEvento);
            if (evento == null || !idEvento.equals(evento.id())) {
                throw servicioNoDisponible("Eventos devolvió una respuesta incompatible");
            }
            return evento;
        } catch (FeignException.NotFound noEncontrado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el evento");
        } catch (FeignException fallo) {
            throw servicioNoDisponible("No se pudo consultar msvc-evento");
        }
    }

    private SectorResponse llamarUbicacion(Long idSector) {
        try {
            SectorResponse sector = sectorClient.obtenerPorId(idSector);
            if (sector == null) {
                throw servicioNoDisponible("Ubicación no devolvió el sector solicitado");
            }
            return sector;
        } catch (FeignException fallo) {
            throw servicioNoDisponible("No se pudo consultar msvc-ubicacion");
        }
    }

    private void validarPropietario(EventoResponse evento, Long idOrganizador) {
        if (evento.idOrganizador() == null) {
            throw servicioNoDisponible("Eventos no devolvió el propietario del evento");
        }
        if (!evento.idOrganizador().equals(idOrganizador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El organizador no es propietario del evento");
        }
    }

    private void validarParametrosInmutables(FilaEvento fila, ActivarFilaRequest request) {
        if (request == null) {
            return;
        }
        boolean maxPersonasModificado = request.maxPersonas() != null
                && !request.maxPersonas().equals(fila.getMaxPersonas());
        boolean cupoModificado = request.cupoEntradas() != null
                && !request.cupoEntradas().equals(fila.getCupoEntradas());
        if (maxPersonasModificado || cupoModificado) {
            throw conflicto("Los parámetros de una fila ACTIVA no se pueden modificar");
        }
    }

    private void validarConfiguracion(int maxPersonas, int cupoEntradas, int capacidad) {
        if (maxPersonas <= 0
                || cupoEntradas <= 0
                || cupoEntradas < maxPersonas
                || (long) cupoEntradas > (long) maxPersonas * 6
                || maxPersonas > capacidad
                || cupoEntradas > capacidad) {
            throw new IllegalArgumentException("La configuración de fila excede sus límites o la disponibilidad");
        }
    }

    private FilaEvento buscarFila(Long idEvento) {
        return filaEventoRepository.findById(idEvento)
                .orElseThrow(() -> new EntityNotFoundException("El evento no tiene una fila persistida"));
    }

    private FilaEvento buscarFilaConBloqueo(Long idEvento) {
        return filaEventoRepository.buscarPorEventoConBloqueo(idEvento)
                .orElseThrow(() -> new EntityNotFoundException("El evento no tiene una fila persistida"));
    }

    private FilaEventoResponse convertirFila(FilaEvento fila) {
        return new FilaEventoResponse(
                fila.getIdEvento(),
                fila.getEstado(),
                fila.getMaxPersonas(),
                fila.getCupoEntradas()
        );
    }

    private IngresoFilaResponse convertirIngreso(IngresoFila ingreso) {
        Long posicion = EN_ESPERA.equals(ingreso.getEstado())
                ? ingresoFilaRepository.countByIdEventoAndEstadoAndSecuenciaLessThan(
                        ingreso.getIdEvento(), EN_ESPERA, ingreso.getSecuencia()
                ) + 1
                : HABILITADO.equals(ingreso.getEstado()) ? 0L : null;
        return new IngresoFilaResponse(
                ingreso.getIdEvento(),
                ingreso.getId(),
                ingreso.getSecuencia(),
                ingreso.getEstado(),
                posicion,
                ingreso.getVenceEn(),
                ingreso.getIdTanda()
        );
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException servicioNoDisponible(String mensaje) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
