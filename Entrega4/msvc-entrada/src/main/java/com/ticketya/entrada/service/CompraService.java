package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.config.FilaVirtualProperties;
import com.ticketya.entrada.dto.request.CompraRequest;
import com.ticketya.entrada.dto.request.EntradaCompraRequest;
import com.ticketya.entrada.dto.request.PagoCompraRequest;
import com.ticketya.entrada.dto.response.CompraResponse;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.PagoCompraResponse;
import com.ticketya.entrada.dto.response.PagoResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.event.CompraPagadaEvent;
import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.IngresoFila;
import com.ticketya.entrada.entity.IntentoCompra;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.PendientePreferencia;
import com.ticketya.entrada.entity.RegistroPago;
import com.ticketya.entrada.entity.RegistroPagoId;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.TandaFila;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.IngresoFilaRepository;
import com.ticketya.entrada.repository.IntentoCompraRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import feign.FeignException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraService.class);
    private static final String EN_ESPERA = "EN_ESPERA";
    private static final String HABILITADO = "HABILITADO";
    private static final String ABIERTA = "ABIERTA";
    private static final String CANCELADA = "CANCELADA";

    private final CompraRepository compraRepository;
    private final EntradaRepository entradaRepository;
    private final FilaEventoRepository filaEventoRepository;
    private final IngresoFilaRepository ingresoFilaRepository;
    private final IntentoCompraRepository intentoCompraRepository;
    private final TandaFilaRepository tandaFilaRepository;
    private final PagoRepository pagoRepository;
    private final RegistroPagoRepository registroPagoRepository;
    private final EventoClient eventoClient;
    private final SectorClient sectorClient;
    private final String resultadoPasarela;
    private final int limitePorCliente;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    /** Outbox opcional: los tests unitarios pueden construir el servicio sin el repositorio. */
    private PendientePreferenciaRepository pendientePreferenciaRepository;

    @Autowired(required = false)
    public void setPendientePreferenciaRepository(PendientePreferenciaRepository pendientePreferenciaRepository) {
        if (pendientePreferenciaRepository != null) {
            this.pendientePreferenciaRepository = pendientePreferenciaRepository;
        }
    }

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

    public CompraService(
            CompraRepository compraRepository,
            EntradaRepository entradaRepository,
            FilaEventoRepository filaEventoRepository,
            IngresoFilaRepository ingresoFilaRepository,
            IntentoCompraRepository intentoCompraRepository,
            TandaFilaRepository tandaFilaRepository,
            PagoRepository pagoRepository,
            RegistroPagoRepository registroPagoRepository,
            EventoClient eventoClient,
            SectorClient sectorClient,
            ApplicationEventPublisher eventPublisher,
            @Value("${ticketya.pasarela.resultado:APRUEBA}") String resultadoPasarela,
            @Value("${ticketya.entrada.limite-por-cliente:6}") int limitePorCliente
    ) {
        this.compraRepository = compraRepository;
        this.entradaRepository = entradaRepository;
        this.filaEventoRepository = filaEventoRepository;
        this.ingresoFilaRepository = ingresoFilaRepository;
        this.intentoCompraRepository = intentoCompraRepository;
        this.tandaFilaRepository = tandaFilaRepository;
        this.pagoRepository = pagoRepository;
        this.registroPagoRepository = registroPagoRepository;
        this.eventoClient = eventoClient;
        this.sectorClient = sectorClient;
        this.eventPublisher = eventPublisher;
        this.resultadoPasarela = resultadoPasarela.toUpperCase(java.util.Locale.ROOT);
        this.limitePorCliente = limitePorCliente;
    }

    @Transactional(readOnly = true)
    public boolean hayComprasEnCurso(Long idEvento) {
        return compraRepository.existsByIdEventoAndEstado(idEvento, Compra.RESERVADA);
    }

    @Transactional(readOnly = true)
    public boolean recibioRechazoDeCupo(Long idUsuario, Long idTanda) {
        return intentoCompraRepository.existsByIdUsuarioAndIdTandaAndResultado(
                idUsuario, idTanda, "CUPO_TANDA_INSUFICIENTE"
        );
    }

    @Transactional
    public CompraResponse crear(Long idUsuario, String clave, CompraRequest request) {
        validarClave(clave);
        String huella = huella(request);

        Compra existente = compraRepository.findByIdUsuarioAndClaveIdempotencia(idUsuario, clave).orElse(null);
        if (existente != null) {
            return devolverMismaSolicitud(existente, huella);
        }
        validarFilaHabilitada();
        validarCantidad(request);

        EventoResponse evento = obtenerEvento(request.idEvento());
        validarSectoresEvento(evento);
        Map<Long, SectorResponse> sectores = precargarSectores(evento, request);
        Map<Long, BigDecimal> precios = precargarPrecios(evento, request);
        validarEventoPublicado(evento);

        filaEventoRepository.bloquearEventoCompartido(request.idEvento());
        filaEventoRepository.bloquearClaveIdempotenciaCompra(idUsuario + ":compra:" + clave);
        existente = compraRepository.findByIdUsuarioAndClaveIdempotencia(idUsuario, clave).orElse(null);
        if (existente != null) {
            return devolverMismaSolicitud(existente, huella);
        }

        FilaEvento fila = filaEventoRepository.buscarPorEventoConBloqueo(request.idEvento())
                .orElseThrow(() -> conflicto("El evento no tiene una fila persistida"));
        Instant ahora = Instant.now();
        TandaFila tanda = tandaFilaRepository.buscarAbiertaPorEventoConBloqueo(request.idEvento()).orElse(null);
        Compra pendiente = vencerCompraPendienteSiCorresponde(idUsuario, request.idEvento(), ahora, tanda);
        if (pendiente != null) {
            return resultadoConflicto("COMPRA_PENDIENTE", null);
        }

        IngresoFila ingreso = null;
        boolean ventaLibre = false;
        if (tanda != null && !CANCELADA.equals(fila.getCausaCierre())) {
            ingreso = ingresoFilaRepository
                    .findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(
                            request.idEvento(), idUsuario, HABILITADO
                    ).orElse(null);
            if (ingreso == null || ingreso.getVenceEn() == null || !ingreso.getVenceEn().isAfter(ahora)
                    || !tanda.getId().equals(ingreso.getIdTanda())) {
                throw conflicto("No hay un turno vigente para iniciar la Compra");
            }
        } else if ("CERRADA".equals(fila.getEstado())
                && "MANUAL".equals(fila.getCausaCierre())
                && tanda == null) {
            ventaLibre = true;
        } else {
            throw conflicto("La fila no admite nuevas Compras en su estado actual");
        }

        Instant venceEn = ventaLibre ? ahora.plus(10, ChronoUnit.MINUTES) : ingreso.getVenceEn();
        List<Compra> reservadas = compraRepository.buscarReservadasDeUsuarioConBloqueo(idUsuario, request.idEvento());
        if (!reservadas.isEmpty()) {
            Compra reservada = reservadas.getFirst();
            if (reservada.getVenceEn().isAfter(ahora)) {
                return resultadoConflicto("COMPRA_PENDIENTE", null);
            }
            if (!vencerCompra(reservada, tanda, ahora)) {
                return resultadoConflicto("COMPRA_PENDIENTE", null);
            }
        }

        bloquearSectores(request.idEvento(), request.entradas());
        CompraRequest resuelta = asignarAsientosLibres(request, sectores, ahora);
        validarSelecciones(resuelta, sectores, entradaRepository, ahora, idUsuario);
        if (entradaRepository.contarActivasDeUsuario(request.idEvento(), idUsuario, ahora)
                + resuelta.entradas().size() > limitePorCliente) {
            throw conflicto("Se superó el límite de entradas por cliente");
        }

        if (tanda != null) {
            int cantidad = resuelta.entradas().size();
            int actualizada = tandaFilaRepository.descontarCupo(tanda.getId(), cantidad);
            if (actualizada == 0) {
                int disponible = tandaFilaRepository.findById(tanda.getId())
                        .map(TandaFila::getCupoRestante).orElse(0);
                registrarIntentoInsuficiente(idUsuario, request.idEvento(), tanda.getId(), ahora);
                reevaluarCierreTrasRechazoDeCupo(fila, tanda, ahora);
                log.info("compra_rechazada_cupo idUsuario={} idEvento={} idTanda={} correlationId={}",
                        idUsuario, request.idEvento(), tanda.getId(), correlationId());
                return resultadoConflicto("CUPO_TANDA_INSUFICIENTE", disponible);
            }
        }

        Compra compra = new Compra();
        compra.setIdUsuario(idUsuario);
        compra.setIdEvento(request.idEvento());
        compra.setIdTanda(tanda == null ? null : tanda.getId());
        compra.setEstado(Compra.RESERVADA);
        compra.setCreadaEn(ahora);
        compra.setVenceEn(venceEn);
        compra.setCantidadEntradas(resuelta.entradas().size());
        compra.setTotal(request.entradas().stream()
                .map(item -> precios.get(item.idSector()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        compra.setClaveIdempotencia(clave);
        compra.setHuellaSolicitud(huella);
        compra = compraRepository.saveAndFlush(compra);

        List<Entrada> entradas = new ArrayList<>();
        for (EntradaCompraRequest seleccion : resuelta.entradas()) {
            Entrada entrada = new Entrada();
            entrada.setIdEvento(request.idEvento());
            entrada.setIdCompra(compra.getIdCompra());
            entrada.setIdUsuario(idUsuario);
            entrada.setIdSector(seleccion.idSector());
            entrada.setFila(seleccion.fila());
            entrada.setAsiento(seleccion.asiento());
            entrada.setPrecio(precios.get(seleccion.idSector()));
            entrada.setEstado("RESERVADA");
            entrada.setExpiraEn(venceEn);
            entradas.add(entrada);
        }
        entradaRepository.saveAll(entradas);
        entradaRepository.flush();
        log.info("compra_reservada idCompra={} idEvento={} idUsuario={} entradas={} correlationId={}",
                compra.getIdCompra(), compra.getIdEvento(), idUsuario, entradas.size(), correlationId());
        return convertir(compra, entradas, null, null);
    }

    @Transactional
    public PagoCompraResponse pagar(Long idCompra, Long idUsuario, String clave, PagoCompraRequest request) {
        validarClave(clave);
        Compra preflight = compraRepository.findByIdCompraAndIdUsuario(idCompra, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        // La consulta de propiedad deja la Compra gestionada con la versión leída antes
        // de esperar los locks. Otra transacción puede confirmarla, vencerla o cancelarla
        // mientras tanto; descartar el estado obsoleto evita StaleObjectStateException y
        // permite que la recarga con bloqueo resuelva la carrera como 409/vencida.
        if (entityManager != null) {
            entityManager.clear();
        }
        filaEventoRepository.bloquearEvento(preflight.getIdEvento());
        filaEventoRepository.bloquearClaveIdempotenciaCompra(idUsuario + ":pago:" + idCompra + ":" + clave);

        Compra compra = compraRepository.findByIdCompraAndIdUsuario(idCompra, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        FilaEvento fila = filaEventoRepository.buscarPorEventoConBloqueo(compra.getIdEvento())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la fila del evento"));
        TandaFila tanda = compra.getIdTanda() == null ? null
                : tandaFilaRepository.buscarPorIdConBloqueo(compra.getIdTanda()).orElse(null);
        compra = compraRepository.buscarPorIdConBloqueo(idCompra)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        Instant ahora = Instant.now();
        Pago pagoExistente = pagoRepository.findByIdCompraAndClaveIdempotencia(idCompra, clave).orElse(null);
        String huellaPago = huellaPago(idCompra, idUsuario, request.medioDePago());
        if (pagoExistente != null) {
            if (!huellaPago.equals(pagoExistente.getHuellaSolicitud())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La clave de idempotencia ya se usó con otros datos");
            }
            if (Compra.RESERVADA.equals(compra.getEstado()) && !compra.getVenceEn().isAfter(ahora)) {
                if (vencerCompra(compra, tanda, ahora)) {
                    reevaluarCierreTanda(fila, tanda, ahora);
                }
                pagoExistente.setEstado("CANCELADO");
                return new PagoCompraResponse(null, idCompra, idUsuario, Compra.VENCIDA,
                        request.medioDePago(), compra.getTotal(), null);
            }
            if (Compra.VENCIDA.equals(compra.getEstado()) || Compra.CANCELADA.equals(compra.getEstado())) {
                return new PagoCompraResponse(null, idCompra, idUsuario, Compra.VENCIDA,
                        request.medioDePago(), compra.getTotal(), null);
            }
            return respuestaPago(pagoExistente, compra);
        }
        if (!Compra.RESERVADA.equals(compra.getEstado())) {
            throw conflicto("La Compra ya no está pendiente de pago");
        }
        if (!compra.getVenceEn().isAfter(ahora)) {
            if (vencerCompra(compra, tanda, ahora)) {
                reevaluarCierreTanda(fila, tanda, ahora);
                return new PagoCompraResponse(null, idCompra, idUsuario, Compra.VENCIDA,
                        request.medioDePago(), compra.getTotal(), null);
            }
            throw conflicto("La Compra ya no está pendiente de pago");
        }

        Pago pago = new Pago();
        pago.setIdUsuario(idUsuario);
        pago.setIdEntrada(null);
        pago.setIdCompra(idCompra);
        pago.setClaveIdempotencia(clave);
        pago.setHuellaSolicitud(huellaPago);
        pago.setMedioDePago(request.medioDePago());
        pago.setEstado("PENDIENTE");
        pago = pagoRepository.saveAndFlush(pago);

        if ("FALLA".equals(resultadoPasarela)) {
            if (compraRepository.rechazarSiVigente(idCompra, ahora) != 1) {
                if (vencerCompra(compra, tanda, Instant.now())) {
                    pago.setEstado("CANCELADO");
                    reevaluarCierreTanda(fila, tanda, ahora);
                    return new PagoCompraResponse(null, idCompra, idUsuario, Compra.VENCIDA,
                            request.medioDePago(), compra.getTotal(), null);
                }
                throw conflicto("La Compra ya no está pendiente de pago");
            }
            List<Entrada> entradasFallo = entradaRepository.findByIdCompraOrderById(idCompra);
            bloquearSectoresDeEntradas(compra.getIdEvento(), entradasFallo);
            pago.setEstado("FALLIDO");
            compra.setEstado(Compra.RECHAZADA);
            compensar(compra, entradasFallo, tanda);
            reevaluarCierreTanda(fila, tanda, ahora);
            log.info("compra_fallo_pasarela idCompra={} idEvento={} idUsuario={} idPago={} correlationId={}",
                    idCompra, compra.getIdEvento(), idUsuario, pago.getIdPago(), correlationId());
            return respuestaPago(pago, compra);
        }
        if ("DEMORA".equals(resultadoPasarela)) {
            log.warn("pago_demorado idCompra={} idEvento={} idUsuario={} idPago={} correlationId={}",
                    idCompra, compra.getIdEvento(), idUsuario, pago.getIdPago(), correlationId());
            return respuestaPago(pago, compra);
        }
        if (!"APRUEBA".equals(resultadoPasarela) && !"RECHAZA".equals(resultadoPasarela)) {
            throw new IllegalStateException("Configuración PASARELA_RESULTADO inválida");
        }
        List<Entrada> entradas = entradaRepository.findByIdCompraOrderById(idCompra);
        bloquearSectoresDeEntradas(compra.getIdEvento(), entradas);
        if ("RECHAZA".equals(resultadoPasarela)) {
            if (compraRepository.rechazarSiVigente(idCompra, ahora) != 1) {
                if (vencerCompra(compra, tanda, Instant.now())) {
                    pago.setEstado("CANCELADO");
                    reevaluarCierreTanda(fila, tanda, ahora);
                    return new PagoCompraResponse(null, idCompra, idUsuario, Compra.VENCIDA,
                            request.medioDePago(), compra.getTotal(), null);
                }
                throw conflicto("La Compra ya no está pendiente de pago");
            }
            pago.setEstado("RECHAZADO");
            compra.setEstado(Compra.RECHAZADA);
            compensar(compra, entradas, tanda);
            reevaluarCierreTanda(fila, tanda, ahora);
            log.info("compra_rechazada idCompra={} idEvento={} idUsuario={} idPago={} correlationId={}",
                    idCompra, compra.getIdEvento(), idUsuario, pago.getIdPago(), correlationId());
            return respuestaPago(pago, compra);
        }

        if (compraRepository.confirmarSiVigente(idCompra, ahora) != 1) {
            if (vencerCompra(compra, tanda, Instant.now())) {
                pago.setEstado("CANCELADO");
                reevaluarCierreTanda(fila, tanda, ahora);
                return new PagoCompraResponse(null, idCompra, idUsuario, Compra.VENCIDA,
                        request.medioDePago(), compra.getTotal(), null);
            }
            throw conflicto("La Compra ya no está pendiente de pago");
        }
        pago.setEstado("APROBADO");
        pago.setReferenciaPasarela("mock-" + UUID.randomUUID());
        compra.setEstado(Compra.PAGADA);
        for (Entrada entrada : entradas) {
            entrada.setEstado("VALIDA");
            entrada.setExpiraEn(null);
            entrada.setCodigoQr(UUID.randomUUID().toString());
            RegistroPagoId registroId = new RegistroPagoId();
            registroId.setIdEntrada(entrada.getId());
            registroId.setIdPago(pago.getIdPago());
            RegistroPago registro = new RegistroPago();
            registro.setId(registroId);
            registro.setEntrada(entrada);
            registro.setPago(pago);
            registro.setFechaPago(LocalDateTime.now(ZoneOffset.UTC));
            registro.setDescripcion("Pago de Compra " + idCompra + " - entrada " + entrada.getId());
            registro.setMontoTotal(entrada.getPrecio());
            registroPagoRepository.save(registro);
        }
        Long idTandaCompra = compra.getIdTanda();
        ingresoFilaRepository.findFirstByIdEventoAndIdUsuarioAndEstadoOrderBySecuenciaDesc(
                compra.getIdEvento(), idUsuario, HABILITADO
        ).filter(m -> idTandaCompra != null && idTandaCompra.equals(m.getIdTanda()))
                .ifPresent(m -> m.setEstado("COMPRO"));
        reevaluarCierreTanda(fila, tanda, ahora);
        registrarPendientesPreferencia(compra, entradas);
        eventPublisher.publishEvent(new CompraPagadaEvent(
                compra.getIdCompra(),
                compra.getIdEvento(),
                compra.getIdUsuario(),
                correlationId(),
                entradas.stream().map(Entrada::getId).toList()
        ));
        log.info("compra_pagada idCompra={} idEvento={} idUsuario={} idPago={} correlationId={}",
                idCompra, compra.getIdEvento(), idUsuario, pago.getIdPago(), correlationId());
        return respuestaPago(pago, compra);
    }

    @Transactional(readOnly = true)
    public CompraResponse consultar(Long idCompra, Long idUsuario) {
        Compra compra = compraRepository.findByIdCompraAndIdUsuario(idCompra, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        return convertir(compra, entradaRepository.findByIdCompraOrderById(idCompra), null, null);
    }

    @Transactional
    public void solicitarDevolucion(Long idCompra, Long idUsuario) {
        Compra preflight = compraRepository.findByIdCompraAndIdUsuario(idCompra, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        EventoResponse evento = obtenerEvento(preflight.getIdEvento());
        if (evento.fechaHora() == null || evento.fechaHora().isBefore(Instant.now().plus(72, ChronoUnit.HOURS))) {
            throw conflicto("La devolución debe solicitarse al menos 72 horas antes del evento");
        }

        filaEventoRepository.bloquearEvento(preflight.getIdEvento());
        FilaEvento fila = filaEventoRepository.buscarPorEventoConBloqueo(preflight.getIdEvento())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la fila del evento"));
        TandaFila tanda = preflight.getIdTanda() == null ? null
                : tandaFilaRepository.buscarPorIdConBloqueo(preflight.getIdTanda()).orElse(null);
        Compra compra = compraRepository.buscarPorIdConBloqueo(idCompra)
                .filter(c -> c.getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        List<Entrada> entradas = entradaRepository.findByIdCompraOrderById(idCompra);
        bloquearSectoresDeEntradas(compra.getIdEvento(), entradas);
        if (Compra.DEVUELTA.equals(compra.getEstado())
                || (!entradas.isEmpty()
                && entradas.stream().allMatch(entrada -> "EN_REINTEGRO".equals(entrada.getEstado())))) {
            return;
        }
        if (!Compra.PAGADA.equals(compra.getEstado())
                || entradas.isEmpty()
                || entradas.stream().anyMatch(entrada -> !"VALIDA".equals(entrada.getEstado())
                || entrada.getEstadoDevolucion() != null)) {
            throw conflicto("La Compra no admite una devolución completa en su estado actual");
        }
        entradas.forEach(entrada -> {
            entrada.setEstado("EN_REINTEGRO");
            if (!"CANCELACION".equals(entrada.getMotivoInvalidacion())) {
                entrada.setMotivoInvalidacion("DEVOLUCION");
            }
            entrada.setEstadoDevolucion("PENDIENTE");
        });
        log.info("devolucion_compra_solicitada idCompra={} idEvento={} idUsuario={} correlationId={}",
                idCompra, compra.getIdEvento(), idUsuario, correlationId());
    }

    @Transactional
    public void resolverDevolucion(Long idCompra, boolean aprobar) {
        Compra compra = bloquearCompra(idCompra);
        List<Entrada> entradas = entradaRepository.findByIdCompraOrderById(idCompra);
        bloquearSectoresDeEntradas(compra.getIdEvento(), entradas);
        if (Compra.DEVUELTA.equals(compra.getEstado())) {
            return;
        }
        if (!Compra.PAGADA.equals(compra.getEstado())
                || entradas.isEmpty()
                || entradas.stream().anyMatch(entrada -> !"EN_REINTEGRO".equals(entrada.getEstado()))) {
            throw conflicto("La Compra no tiene una devolución pendiente");
        }
        if (!aprobar) {
            if (pagoRepository.findFirstByIdCompraAndEstado(idCompra, "APROBADO").isEmpty()) {
                throw conflicto("No se puede rechazar un reintegro ya iniciado");
            }
            entradas.forEach(entrada -> {
                entrada.setEstado("VALIDA");
                entrada.setMotivoInvalidacion(null);
                entrada.setEstadoDevolucion("RECHAZADA");
            });
            log.info("devolucion_compra_rechazada idCompra={} idEvento={} correlationId={}",
                    idCompra, compra.getIdEvento(), correlationId());
            return;
        }
        if (entradas.stream().anyMatch(entrada -> "USADA".equals(entrada.getEstado()))) {
            throw conflicto("No se puede devolver una Compra con entradas usadas");
        }
        ejecutarReintegroCompleto(compra, entradas, "DEVOLUCION");
    }

    @Transactional
    public void reintentarDevolucion(Long idCompra) {
        Compra compra = bloquearCompra(idCompra);
        if (Compra.DEVUELTA.equals(compra.getEstado())) {
            return;
        }
        List<Entrada> entradas = entradaRepository.findByIdCompraOrderById(idCompra);
        bloquearSectoresDeEntradas(compra.getIdEvento(), entradas);
        Pago pago = pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(idCompra)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el pago de la Compra"));
        if (!"REINTEGRO_PENDIENTE".equals(pago.getEstado())
                || entradas.isEmpty()
                || entradas.stream().anyMatch(entrada -> !"EN_REINTEGRO".equals(entrada.getEstado()))) {
            throw conflicto("La Compra no tiene un reintegro pendiente");
        }
        ejecutarReintegroCompleto(compra, entradas, "DEVOLUCION");
        log.info("devolucion_compra_reintentada idCompra={} idEvento={} correlationId={}",
                idCompra, compra.getIdEvento(), correlationId());
    }

    private Compra bloquearCompra(Long idCompra) {
        Compra preflight = compraRepository.findById(idCompra)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
        filaEventoRepository.bloquearEvento(preflight.getIdEvento());
        filaEventoRepository.buscarPorEventoConBloqueo(preflight.getIdEvento())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la fila del evento"));
        if (preflight.getIdTanda() != null) {
            tandaFilaRepository.buscarPorIdConBloqueo(preflight.getIdTanda());
        }
        return compraRepository.buscarPorIdConBloqueo(idCompra)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la Compra"));
    }

    private String motivoConservado(Entrada entrada, String motivoPorDefecto) {
        return entrada.getMotivoInvalidacion() == null ? motivoPorDefecto : entrada.getMotivoInvalidacion();
    }

    private void ejecutarReintegroCompleto(Compra compra, List<Entrada> entradas, String motivo) {
        Pago pago = pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(compra.getIdCompra())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el pago de la Compra"));
        if (!"APROBADO".equals(pago.getEstado()) && !"REINTEGRO_PENDIENTE".equals(pago.getEstado())) {
            throw conflicto("El pago de la Compra no admite reintegro");
        }
        if (compra.getClaveReintegro() == null) {
            compra.setClaveReintegro("devolucion-compra-" + compra.getIdCompra());
        }
        if ("FALLA".equals(resultadoPasarela)) {
            pago.setEstado("REINTEGRO_PENDIENTE");
            entradas.forEach(entrada -> {
                entrada.setEstado("EN_REINTEGRO");
                entrada.setMotivoInvalidacion(motivoConservado(entrada, motivo));
            });
            return;
        }
        if (!"APRUEBA".equals(resultadoPasarela)) {
            throw new IllegalStateException("Configuración PASARELA_RESULTADO inválida para reintegros");
        }
        pago.setEstado("REEMBOLSADO");
        pago.setReferenciaPasarela("refund-" + compra.getClaveReintegro());
        compra.setEstado(Compra.DEVUELTA);
        entradas.forEach(entrada -> {
            entrada.setEstado("DEVUELTA");
            entrada.setMotivoInvalidacion(motivoConservado(entrada, motivo));
            entrada.setEstadoDevolucion("APROBADA");
            entrada.setCodigoQr(null);
        });
        log.info("devolucion_compra_aprobada idCompra={} idEvento={} idPago={} correlationId={}",
                compra.getIdCompra(), compra.getIdEvento(), pago.getIdPago(), correlationId());
    }

    @Transactional
    public void vencerComprasTanda(Long idEvento, Long idTanda, Instant ahora) {
        List<Compra> vencidas = compraRepository.findByIdEventoAndEstado(idEvento, Compra.RESERVADA).stream()
                .filter(c -> idTanda.equals(c.getIdTanda()))
                .sorted(Comparator.comparing(Compra::getIdCompra))
                .toList();
        TandaFila tanda = tandaFilaRepository.buscarPorIdConBloqueo(idTanda).orElse(null);
        for (Compra compra : vencidas) {
            Compra bloqueada = compraRepository.buscarPorIdConBloqueo(compra.getIdCompra()).orElse(null);
            if (bloqueada != null && !bloqueada.getVenceEn().isAfter(ahora)) {
                vencerCompra(bloqueada, tanda, ahora);
            }
        }
    }

    @Transactional
    public void expirarComprasVencidas(Long idEvento, Instant ahora) {
        filaEventoRepository.bloquearEvento(idEvento);
        filaEventoRepository.buscarPorEventoConBloqueo(idEvento).ifPresent(fila -> {
            List<Compra> candidatas = compraRepository.findByIdEventoAndEstado(idEvento, Compra.RESERVADA).stream()
                    .filter(compra -> !compra.getVenceEn().isAfter(ahora))
                    .sorted(Comparator.comparing(Compra::getIdCompra))
                    .toList();
            candidatas.stream().map(Compra::getIdTanda).filter(java.util.Objects::nonNull).distinct().sorted()
                    .forEach(idTanda -> tandaFilaRepository.buscarPorIdConBloqueo(idTanda));
            List<Compra> bloqueadas = candidatas.stream()
                    .map(c -> compraRepository.buscarPorIdConBloqueo(c.getIdCompra()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            List<Entrada> entradas = bloqueadas.stream()
                    .flatMap(c -> entradaRepository.findByIdCompraOrderById(c.getIdCompra()).stream())
                    .toList();
            bloquearSectoresDeEntradas(idEvento, entradas);
            for (Compra compra : bloqueadas) {
                TandaFila tanda = compra.getIdTanda() == null ? null
                        : tandaFilaRepository.findById(compra.getIdTanda()).orElse(null);
                List<Entrada> entradasCompra = entradas.stream()
                        .filter(entrada -> compra.getIdCompra().equals(entrada.getIdCompra()))
                        .toList();
                if (Compra.RESERVADA.equals(compra.getEstado()) && !compra.getVenceEn().isAfter(ahora)) {
                    vencerCompra(compra, tanda, ahora);
                }
            }
        });
    }

    @Transactional
    public void cancelarComprasPendientesEvento(Long idEvento) {
        List<Compra> candidatas = java.util.stream.Stream.of(Compra.RESERVADA, Compra.PAGADA)
                .flatMap(estado -> compraRepository.findByIdEventoAndEstado(idEvento, estado).stream())
                .sorted(Comparator.comparing(Compra::getIdCompra))
                .toList();
        candidatas.stream().map(Compra::getIdTanda).filter(java.util.Objects::nonNull).distinct().sorted()
                .forEach(idTanda -> tandaFilaRepository.buscarPorIdConBloqueo(idTanda));
        List<Compra> bloqueadas = candidatas.stream()
                .map(c -> compraRepository.buscarPorIdConBloqueo(c.getIdCompra()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<Entrada> entradas = bloqueadas.stream()
                .flatMap(c -> entradaRepository.findByIdCompraOrderById(c.getIdCompra()).stream())
                .toList();
        bloquearSectoresDeEntradas(idEvento, entradas);
        for (Compra compra : bloqueadas) {
            if (Compra.RESERVADA.equals(compra.getEstado())) {
                if (compraRepository.cancelarSiReservada(compra.getIdCompra()) != 1) {
                    continue;
                }
                compra.setEstado(Compra.CANCELADA);
                TandaFila tanda = compra.getIdTanda() == null ? null
                        : tandaFilaRepository.findById(compra.getIdTanda()).orElse(null);
                List<Entrada> entradasCompra = entradas.stream()
                        .filter(entrada -> compra.getIdCompra().equals(entrada.getIdCompra()))
                        .toList();
                pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(compra.getIdCompra())
                        .filter(pago -> "PENDIENTE".equals(pago.getEstado()))
                        .ifPresent(pago -> pago.setEstado("CANCELADO"));
                compensar(compra, entradasCompra, tanda);
            } else if (Compra.PAGADA.equals(compra.getEstado())) {
                devolverCompraPorCancelacion(compra, entradas.stream()
                        .filter(entrada -> compra.getIdCompra().equals(entrada.getIdCompra()))
                        .toList());
            }
        }
    }

    private void devolverCompraPorCancelacion(Compra compra, List<Entrada> entradas) {
        Pago pago = pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(compra.getIdCompra()).orElse(null);
        entradas.forEach(entrada -> {
            entrada.setEstado("EN_REINTEGRO");
            entrada.setCodigoQr(null);
            entrada.setMotivoInvalidacion("CANCELACION");
        });
        if (pago == null || (!"APROBADO".equals(pago.getEstado())
                && !"REINTEGRO_PENDIENTE".equals(pago.getEstado()))) {
            return;
        }
        if (compra.getClaveReintegro() == null) {
            compra.setClaveReintegro("cancelacion-compra-" + compra.getIdCompra());
        }
        ejecutarReintegroCompleto(compra, entradas, "CANCELACION");
    }
    private Compra vencerCompraPendienteSiCorresponde(
            Long idUsuario,
            Long idEvento,
            Instant ahora,
            TandaFila tandaAbierta
    ) {
        List<Compra> compras = compraRepository.buscarReservadasDeUsuarioConBloqueo(idUsuario, idEvento);
        if (compras.isEmpty()) {
            return null;
        }
        Compra compra = compras.getFirst();
        if (compra.getVenceEn().isAfter(ahora)) {
            return compra;
        }
        TandaFila tanda = tandaAbierta;
        if (compra.getIdTanda() != null && (tanda == null || !tanda.getId().equals(compra.getIdTanda()))) {
            tanda = tandaFilaRepository.buscarPorIdConBloqueo(compra.getIdTanda()).orElse(null);
        }
        vencerCompra(compra, tanda, ahora);
        return null;
    }

    private boolean vencerCompra(Compra compra, TandaFila tanda, Instant ahora) {
        if (!Compra.RESERVADA.equals(compra.getEstado()) || compra.getVenceEn().isAfter(ahora)) {
            return false;
        }
        if (compraRepository.vencerSiVencida(compra.getIdCompra(), ahora) != 1) {
            return false;
        }
        bloquearSectoresDeEntradas(compra.getIdEvento(),
                entradaRepository.findByIdCompraOrderById(compra.getIdCompra()));
        pagoRepository.findFirstByIdCompraOrderByIdPagoDesc(compra.getIdCompra())
                .filter(pago -> "PENDIENTE".equals(pago.getEstado()))
                .ifPresent(pago -> pago.setEstado("CANCELADO"));
        compra.setEstado(Compra.VENCIDA);
        compensar(compra, entradaRepository.findByIdCompraOrderById(compra.getIdCompra()), tanda);
        log.info("compra_vencida idCompra={} idEvento={} correlationId={}",
                compra.getIdCompra(), compra.getIdEvento(), correlationId());
        return true;
    }

    private String correlationId() {
        String correlationId = MDC.get(com.ticketya.entrada.config.CorrelationContext.MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "no-disponible" : correlationId;
    }

    /**
     * Registra el outbox de preferencias de la compra agrupada. El titular es el usuario que pago,
     * no los participantes invitados. Se llama dentro de la transaccion de pago, antes de publicar
     * el evento, para que el aviso y el pago se confirmen juntos.
     */
    private void registrarPendientesPreferencia(Compra compra, List<Entrada> entradas) {
        if (pendientePreferenciaRepository == null) {
            return;
        }
        String correlationId = correlationId();
        for (Entrada entrada : entradas) {
            if (pendientePreferenciaRepository.findByIdEntrada(entrada.getId()).isPresent()) {
                continue;
            }
            PendientePreferencia pendiente = new PendientePreferencia();
            pendiente.setIdEntrada(entrada.getId());
            pendiente.setIdUsuario(compra.getIdUsuario());
            pendiente.setIdEvento(compra.getIdEvento());
            pendiente.setCorrelationId(correlationId);
            pendiente.setEstado(PendientePreferencia.PENDIENTE);
            pendiente.setIntentos(0);
            pendientePreferenciaRepository.save(pendiente);
        }
    }

    private void compensar(Compra compra, List<Entrada> entradas, TandaFila tanda) {
        entradas.forEach(entrada -> {
            entrada.setEstado(CANCELADA);
            entrada.setExpiraEn(null);
            entrada.setCodigoQr(null);
        });
        if (tanda != null && ABIERTA.equals(tanda.getEstado())) {
            tanda.setCupoRestante(Math.min(
                    tanda.getCupoEfectivo(),
                    tanda.getCupoRestante() + compra.getCantidadEntradas()
            ));
        }
    }

    private void registrarIntentoInsuficiente(Long idUsuario, Long idEvento, Long idTanda, Instant ahora) {
        IntentoCompra intento = new IntentoCompra();
        intento.setIdUsuario(idUsuario);
        intento.setIdEvento(idEvento);
        intento.setIdTanda(idTanda);
        intento.setCreadoEn(ahora);
        intento.setResultado("CUPO_TANDA_INSUFICIENTE");
        intentoCompraRepository.save(intento);
    }

    private void reevaluarCierreTrasRechazoDeCupo(FilaEvento fila, TandaFila tanda, Instant ahora) {
        // descontarCupo limpia el EntityManager; recargar con bloqueo para que el cierre
        // actúe sobre entidades gestionadas por esta misma transacción.
        FilaEvento filaVigente = filaEventoRepository.buscarPorEventoConBloqueo(fila.getIdEvento())
                .orElse(fila);
        TandaFila tandaVigente = tandaFilaRepository.buscarPorIdConBloqueo(tanda.getId())
                .orElse(tanda);
        reevaluarCierreTanda(filaVigente, tandaVigente, ahora);
    }

    private void reevaluarCierreTanda(FilaEvento fila, TandaFila tanda, Instant ahora) {
        if (tanda == null || !ABIERTA.equals(tanda.getEstado())
                || compraRepository.existsByIdEventoAndIdTandaAndEstado(
                        fila.getIdEvento(), tanda.getId(), Compra.RESERVADA
                )) {
            return;
        }
        List<IngresoFila> habilitados = ingresoFilaRepository.buscarHabilitadosDeTanda(
                fila.getIdEvento(), tanda.getId()
        );
        boolean cupoAgotado = tanda.getCupoRestante() == 0;
        boolean resuelta = habilitados.isEmpty();
        if (!cupoAgotado && !resuelta) {
            return;
        }
        if (cupoAgotado) {
            habilitados.forEach(ingreso -> {
                ingreso.setEstado(EN_ESPERA);
                ingreso.setVenceEn(null);
            });
        }
        tanda.setEstado("CERRADA");
        tanda.setCerradaEn(ahora);
        if (!"ACTIVA".equals(fila.getEstado())) {
            return;
        }

        long disponibles = Math.max(0L, (long) fila.getCapacidadTotal()
                - entradaRepository.contarEntradasActivasDeEvento(fila.getIdEvento(), ahora));
        if (disponibles == 0) {
            if (!hayComprasEnCurso(fila.getIdEvento())) {
                fila.setEstado("AGOTADA");
            }
            return;
        }
        if (!ingresoFilaRepository.existsByIdEventoAndEstado(fila.getIdEvento(), EN_ESPERA)) {
            return;
        }
        TandaFila anterior = tandaFilaRepository.findFirstByIdEventoOrderByNumeroDesc(fila.getIdEvento())
                .orElse(tanda);
        int cupoEfectivo = (int) Math.min((long) fila.getCupoEntradas() + anterior.getCupoRestante(), disponibles);
        if (cupoEfectivo <= 0) {
            if (!hayComprasEnCurso(fila.getIdEvento())) {
                fila.setEstado("AGOTADA");
            }
            return;
        }
        TandaFila siguiente = new TandaFila();
        siguiente.setIdEvento(fila.getIdEvento());
        siguiente.setNumero(anterior.getNumero() + 1);
        siguiente.setEstado(ABIERTA);
        siguiente.setIniciadaEn(ahora);
        siguiente.setVenceEn(ahora.plus(10, ChronoUnit.MINUTES));
        siguiente.setCupoEfectivo(cupoEfectivo);
        siguiente.setCupoRestante(cupoEfectivo);
        siguiente = tandaFilaRepository.saveAndFlush(siguiente);
        List<IngresoFila> esperando = ingresoFilaRepository.findByIdEventoAndEstadoOrderBySecuenciaAsc(
                fila.getIdEvento(), EN_ESPERA, org.springframework.data.domain.PageRequest.of(0, fila.getMaxPersonas())
        );
        for (IngresoFila ingreso : esperando) {
            ingreso.setEstado(HABILITADO);
            ingreso.setIdTanda(siguiente.getId());
            ingreso.setVenceEn(siguiente.getVenceEn());
        }
    }

    private Map<Long, SectorResponse> precargarSectores(EventoResponse evento, CompraRequest request) {
        Set<Long> sectoresEvento = evento.sectores().stream()
                .map(EventoSectorResponse::idSector)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, SectorResponse> resultado = new HashMap<>();
        for (Long idSector : request.entradas().stream().map(EntradaCompraRequest::idSector).distinct().toList()) {
            if (!sectoresEvento.contains(idSector)) {
                throw conflicto("El sector no pertenece al evento");
            }
            SectorResponse sector;
            try {
                sector = sectorClient.obtenerPorId(idSector);
            } catch (FeignException fallo) {
                throw servicioNoDisponible("No se pudo consultar msvc-ubicacion");
            }
            if (sector == null || !idSector.equals(sector.idSector())
                    || sector.capacidadMax() == null || sector.capacidadMax() <= 0
                    || sector.tipo() == null) {
                throw servicioNoDisponible("Ubicación devolvió datos de sector inválidos");
            }
            resultado.put(idSector, sector);
        }
        return resultado;
    }

    private Map<Long, BigDecimal> precargarPrecios(EventoResponse evento, CompraRequest request) {
        Map<Long, BigDecimal> resultado = new HashMap<>();
        for (Long idSector : request.entradas().stream().map(EntradaCompraRequest::idSector).distinct().toList()) {
            EventoSectorResponse sector = evento.sectores().stream()
                    .filter(s -> idSector.equals(s.idSector()))
                    .findFirst()
                    .orElseThrow(() -> conflicto("El sector no pertenece al evento"));
            if (sector.precio() == null || sector.precio().signum() < 0) {
                throw servicioNoDisponible("Eventos devolvió un precio inválido para el sector");
            }
            resultado.put(idSector, sector.precio());
        }
        return resultado;
    }

    private void validarSectoresEvento(EventoResponse evento) {
        if (evento.sectores() == null || evento.sectores().isEmpty()
                || evento.sectores().stream().anyMatch(sector -> sector == null || sector.idSector() == null)) {
            throw servicioNoDisponible("Eventos devolvió sectores incompletos");
        }
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

    private void validarEventoPublicado(EventoResponse evento) {
        if (!"PUBLICADO".equalsIgnoreCase(evento.estado())) {
            throw conflicto("Solo se puede comprar para un evento PUBLICADO");
        }
    }

    private void bloquearSectores(Long idEvento, List<EntradaCompraRequest> entradas) {
        entradas.stream().map(EntradaCompraRequest::idSector).distinct().sorted()
                .forEach(idSector -> entradaRepository.bloquearReservasPorClave(idEvento + ":" + idSector));
    }

    private void bloquearSectoresDeEntradas(Long idEvento, List<Entrada> entradas) {
        entradas.stream().map(Entrada::getIdSector).distinct().sorted()
                .forEach(idSector -> entradaRepository.bloquearReservasPorClave(idEvento + ":" + idSector));
    }

    /** Completa fila y asiento de los sectores numerados que el cliente eligió sin indicar asiento. */
    private CompraRequest asignarAsientosLibres(
            CompraRequest request,
            Map<Long, SectorResponse> sectores,
            Instant ahora
    ) {
        try {
            return AsignacionAsientos.resolver(request, sectores, idSector -> {
                Set<String> claves = new HashSet<>();
                for (Object[] fila : entradaRepository.asientosOcupadosDeSector(request.idEvento(), idSector, ahora)) {
                    claves.add(fila[0] + ":" + fila[1]);
                }
                return claves;
            });
        } catch (AsignacionAsientos.SinLugaresException exception) {
            throw conflicto("El sector no tiene lugares suficientes");
        } catch (AsignacionAsientos.DimensionesInvalidasException exception) {
            throw servicioNoDisponible("Ubicación devolvió dimensiones inválidas para el sector numerado");
        }
    }

    private void validarSelecciones(
            CompraRequest request,
            Map<Long, SectorResponse> sectores,
            EntradaRepository repository,
            Instant ahora,
            Long idUsuario
    ) {
        Set<String> asientos = new HashSet<>();
        Map<Long, Integer> cantidades = new HashMap<>();
        for (EntradaCompraRequest seleccion : request.entradas()) {
            SectorResponse sector = sectores.get(seleccion.idSector());
            cantidades.merge(seleccion.idSector(), 1, Integer::sum);
            boolean numerado = "NUMERADO".equalsIgnoreCase(sector.tipo());
            if (numerado) {
                if (seleccion.fila() == null || seleccion.asiento() == null
                        || seleccion.fila() <= 0 || seleccion.asiento() <= 0
                        || sector.filas() == null || sector.asientosPorFila() == null
                        || seleccion.fila() > sector.filas() || seleccion.asiento() > sector.asientosPorFila()) {
                    throw new IllegalArgumentException("La selección de asiento numerado es inválida");
                }
                String clave = seleccion.idSector() + ":" + seleccion.fila() + ":" + seleccion.asiento();
                if (!asientos.add(clave)) {
                    throw new IllegalArgumentException("La Compra repite un asiento");
                }
                if (repository.contarAsignacionesActivasDeAsiento(
                        request.idEvento(), seleccion.idSector(), seleccion.fila(), seleccion.asiento(), ahora
                ) > 0) {
                    throw conflicto("El asiento seleccionado ya está reservado o vendido");
                }
            } else if ("NO_NUMERADO".equalsIgnoreCase(sector.tipo())) {
                if (seleccion.fila() != null || seleccion.asiento() != null) {
                    throw new IllegalArgumentException("No se indican fila ni asiento para un sector no numerado");
                }
            } else {
                throw servicioNoDisponible("Ubicación devolvió un tipo de sector inválido");
            }
        }
        for (Map.Entry<Long, Integer> cantidad : cantidades.entrySet()) {
            SectorResponse sector = sectores.get(cantidad.getKey());
            long ocupadas = repository.contarActivasDeSector(request.idEvento(), cantidad.getKey(), ahora);
            if (ocupadas + cantidad.getValue() > sector.capacidadMax()) {
                throw conflicto("El sector no tiene lugares suficientes");
            }
        }
    }

    private void validarCantidad(CompraRequest request) {
        if (request == null || request.idEvento() == null || request.entradas() == null
                || request.entradas().isEmpty() || request.entradas().size() > 6
                || request.entradas().stream().anyMatch(entrada -> entrada == null || entrada.idSector() == null)) {
            throw new IllegalArgumentException("La Compra debe incluir entre una y seis selecciones válidas");
        }
    }

    private String huella(CompraRequest request) {
        if (request == null) {
            return sha256("null");
        }
        String payload = request.idEvento() + "|" + (request.entradas() == null ? "null"
                : request.entradas().stream()
                .map(e -> e == null ? "null" : e.idSector() + ":" + e.fila() + ":" + e.asiento())
                .collect(java.util.stream.Collectors.joining("|")));
        return sha256(payload);
    }

    private String huellaPago(Long idCompra, Long idUsuario, String medio) {
        return sha256(idCompra + "|" + idUsuario + "|" + medio.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private CompraResponse devolverMismaSolicitud(Compra compra, String huella) {
        if (!huella.equals(compra.getHuellaSolicitud())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La clave de idempotencia ya se usó con otros datos");
        }
        return convertir(compra, entradaRepository.findByIdCompraOrderById(compra.getIdCompra()), null, null);
    }

    private CompraResponse resultadoConflicto(String codigo, Integer disponible) {
        return new CompraResponse(null, null, null, null, null, List.of(), codigo, disponible);
    }

    private CompraResponse convertir(Compra compra, List<Entrada> entradas, String codigo, Integer disponible) {
        return new CompraResponse(
                compra.getIdCompra(),
                compra.getIdEvento(),
                compra.getEstado(),
                compra.getTotal(),
                compra.getVenceEn(),
                entradas.stream().map(entrada -> new EntradaResponse(
                        entrada.getId(), entrada.getIdEvento(), entrada.getIdUsuario(), entrada.getIdSector(),
                        entrada.getFila(), entrada.getAsiento(), entrada.getPrecio(), entrada.getCodigoQr(),
                        entrada.getEstado(), entrada.getExpiraEn(), entrada.getIdCompra(),
                        entrada.getEstadoDevolucion(), entrada.getMotivoInvalidacion()
                )).toList(),
                codigo,
                disponible
        );
    }

    private PagoCompraResponse respuestaPago(Pago pago, Compra compra) {
        return new PagoCompraResponse(
                pago.getIdPago(),
                compra.getIdCompra(),
                pago.getIdUsuario(),
                compra.getEstado(),
                pago.getMedioDePago(),
                compra.getTotal(),
                pago.getReferenciaPasarela()
        );
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key es obligatorio y admite hasta 100 caracteres");
        }
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException servicioNoDisponible(String mensaje) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
