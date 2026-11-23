package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.dto.request.EntradaRequest;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.dto.response.EventoResponse;
import com.ticketya.entrada.dto.response.EventoSectorResponse;
import com.ticketya.entrada.dto.response.SectorResponse;
import com.ticketya.entrada.dto.response.UsuarioResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.exception.QrError;
import com.ticketya.entrada.exception.QrRechazadoException;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EntradaService {

    private static final Logger log = LoggerFactory.getLogger(EntradaService.class);

    private final EntradaRepository entradaRepository;
    private final FilaEventoRepository filaEventoRepository;
    private final UsuarioClient usuarioClient;
    private final EventoClient eventoClient;
    private final SectorClient sectorClient;
    private final int limitePorCliente;
    private final ObjectProvider<CompraService> compraService;

    @Autowired
    public EntradaService(
            EntradaRepository entradaRepository,
            FilaEventoRepository filaEventoRepository,
            UsuarioClient usuarioClient,
            EventoClient eventoClient,
            SectorClient sectorClient,
            ObjectProvider<CompraService> compraService,
            @Value("${ticketya.entrada.limite-por-cliente:6}") int limitePorCliente
    ) {
        this.entradaRepository = entradaRepository;
        this.filaEventoRepository = filaEventoRepository;
        this.usuarioClient = usuarioClient;
        this.eventoClient = eventoClient;
        this.sectorClient = sectorClient;
        this.compraService = compraService;
        this.limitePorCliente = limitePorCliente;
    }

    public EntradaService(
            EntradaRepository entradaRepository,
            FilaEventoRepository filaEventoRepository,
            UsuarioClient usuarioClient,
            EventoClient eventoClient,
            SectorClient sectorClient,
            int limitePorCliente
    ) {
        this(entradaRepository, filaEventoRepository, usuarioClient, eventoClient, sectorClient, null,
                limitePorCliente);
    }

    @Transactional
    public EntradaResponse crear(EntradaRequest request) {
        validarClaveIdempotencia(request.getClaveIdempotencia());

        // Se reutiliza siempre la primera entrada para esta clave, sin comparar los datos del pedido.
        entradaRepository.bloquearSolicitudIdempotente(
                request.getIdUsuario() + ":" + request.getClaveIdempotencia()
        );
        Entrada existente = entradaRepository.findByIdUsuarioAndClaveIdempotencia(
                request.getIdUsuario(),
                request.getClaveIdempotencia()
        ).orElse(null);
        if (existente != null) {
            return convertirRespuesta(existente);
        }

        // Se leen los datos de sus servicios propietarios para no aceptar capacidad,
        // fecha o existencia de usuario suministrados por quien solicita la reserva.
        UsuarioResponse usuario = usuarioClient.obtenerPorId(request.getIdUsuario());
        EventoResponse evento = eventoClient.obtenerPorId(request.getIdEvento());
        SectorResponse sector = sectorClient.obtenerPorId(request.getIdSector());
        validarRespuestaRemota(usuario == null || !request.getIdUsuario().equals(usuario.idUsuario()),
                "Usuarios no devolvió el usuario solicitado");
        validarRespuestaRemota(evento == null || !request.getIdEvento().equals(evento.id()),
                "Eventos no devolvió el evento solicitado");
        validarRespuestaRemota(sector == null || !request.getIdSector().equals(sector.idSector()),
                "Ubicación no devolvió el sector solicitado");
        EventoSectorResponse precioSector = eventoClient.obtenerPrecioSector(
                request.getIdEvento(),
                request.getIdSector()
        );
        validarRespuestaRemota(precioSector == null
                        || !request.getIdSector().equals(precioSector.idSector())
                        || precioSector.precio() == null
                        || precioSector.precio().signum() < 0,
                "Eventos no devolvió un precio válido para el sector del evento");
        validarReserva(request, evento, sector);
        Instant ahora = Instant.now();
        // El lock compartido se toma antes de consultar la fila y se conserva hasta
        // el commit de este método @Transactional, coordinando con su activación exclusiva.
        filaEventoRepository.bloquearEventoCompartido(request.getIdEvento());
        if (filaEventoRepository.existsById(request.getIdEvento())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El evento tiene una fila persistida; debe utilizar una Compra agrupada"
            );
        }
        entradaRepository.vencerReservas(ahora);

        // Este segundo bloqueo protege el conteo de capacidad del evento y sector.
        entradaRepository.bloquearReservasPorClave(
                request.getIdEvento() + ":" + request.getIdSector()
        );

        if (entradaRepository.contarActivasDeSector(request.getIdEvento(), request.getIdSector(), ahora)
                >= sector.capacidadMax()) {
            throw new IllegalStateException("El sector no tiene lugares disponibles (409)");
        }
        if (entradaRepository.contarActivasDeUsuario(request.getIdEvento(), request.getIdUsuario(), ahora)
                >= limitePorCliente) {
            throw new IllegalStateException("Se superó el límite de entradas por cliente (409)");
        }
        if (request.getFila() != null
                && entradaRepository.contarAsignacionesActivasDeAsiento(
                        request.getIdEvento(),
                        request.getIdSector(),
                        request.getFila(),
                        request.getAsiento(),
                        ahora
                ) > 0) {
            throw new IllegalStateException("El asiento seleccionado ya está reservado o vendido (409)");
        }

        Entrada entrada = new Entrada();
        aplicarDatos(entrada, request);
        // El precio enviado por el cliente no es confiable; se guarda el precio oficial como snapshot de la reserva.
        entrada.setPrecio(precioSector.precio());
        entrada.setEstado("RESERVADA");
        entrada.setExpiraEn(ahora.plus(10, ChronoUnit.MINUTES));
        entrada.setClaveIdempotencia(request.getClaveIdempotencia());
        entrada.setCodigoQr(null);
        entrada = entradaRepository.save(entrada);
        return convertirRespuesta(entrada);
    }

    public List<EntradaResponse> listar() {
        return entradaRepository.findAll().stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Transactional
    public List<EntradaResponse> listarDeUsuario(Long idUsuario) {
        entradaRepository.vencerReservas(Instant.now());
        return entradaRepository.findByIdUsuarioOrderByIdDesc(idUsuario).stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    public EntradaResponse buscarPorId(Long id) {
        return entradaRepository.findById(id)
                .map(this::convertirRespuesta)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entrada con id " + id));
    }

    @Transactional
    public EntradaResponse buscarPorIdDeUsuario(Long id, Long idUsuario) {
        entradaRepository.vencerReservas(Instant.now());
        return entradaRepository.findByIdAndIdUsuario(id, idUsuario)
                .map(this::convertirRespuesta)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entrada con id " + id));
    }

    @Transactional
    public void solicitarDevolucion(Long id, Long idUsuario) {
        Entrada entrada = entradaRepository.findByIdAndIdUsuario(id, idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entrada con id " + id));
        if (entrada.getIdCompra() != null) {
            List<Entrada> entradasCompra = entradaRepository.findByIdCompraOrderById(entrada.getIdCompra());
            CompraService servicioCompra = compraService == null ? null : compraService.getIfAvailable();
            if (entradasCompra.size() == 1 && servicioCompra != null) {
                servicioCompra.solicitarDevolucion(entrada.getIdCompra(), idUsuario);
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La devolución debe solicitarse para la Compra completa"
            );
        }
        if (!"VALIDA".equals(entrada.getEstado())) {
            throw new IllegalStateException("Solo se pueden solicitar devoluciones de entradas válidas");
        }
        EventoResponse evento = eventoClient.obtenerPorId(entrada.getIdEvento());
        validarRespuestaRemota(evento == null || !entrada.getIdEvento().equals(evento.id())
                        || evento.fechaHora() == null,
                "Eventos no devolvió la fecha del evento");
        Instant fechaEvento = evento.fechaHora();
        if (fechaEvento.isBefore(Instant.now().plus(72, ChronoUnit.HOURS))) {
            throw new IllegalStateException("La devolución debe solicitarse al menos 72 horas antes del evento");
        }
        if (entrada.getEstadoDevolucion() != null) {
            throw new IllegalStateException("La solicitud de devolución ya fue registrada");
        }
        entrada.setEstadoDevolucion("PENDIENTE");
    }

    @Transactional
    public boolean resolverDevolucion(Long id, boolean aprobar) {
        Entrada entrada = buscarEntidadPorId(id);
        if (!"PENDIENTE".equals(entrada.getEstadoDevolucion())) {
            throw new IllegalStateException("La solicitud de devolución no está pendiente");
        }
        entrada.setEstadoDevolucion(aprobar ? "APROBADA" : "RECHAZADA");
        if (aprobar) {
            entrada.setEstado("EN_REINTEGRO");
            entrada.setMotivoInvalidacion("DEVOLUCION");
        }
        return aprobar;
    }

    @Transactional
    public void cancelarEvento(Long idEvento) {
        entradaRepository.vencerReservas(Instant.now());
        // Las reservas aún no pagadas se invalidan sin generar un reintegro.
        entradaRepository.cancelarReservasDeEvento(idEvento);
        // Las entradas emitidas dejan de ser válidas y pasan al flujo de reintegro.
        entradaRepository.marcarEntradasDeEventoEnReintegro(idEvento);
    }

    @Transactional
    public EntradaResponse validarQr(String qr, Long idEvento, Long idPersonal) {
        if (qr == null || qr.isBlank()) {
            throw rechazar(QrError.QR_REQUEST_INVALIDO, idEvento, idPersonal);
        }
        Entrada entrada = entradaRepository.findByCodigoQr(qr)
                .orElseThrow(() -> rechazar(QrError.QR_NO_EXISTE, idEvento, idPersonal));
        if (!entrada.getIdEvento().equals(idEvento)) {
            throw rechazar(QrError.QR_EVENTO_DISTINTO, idEvento, idPersonal);
        }
        QrError motivoRechazo = motivoRechazo(entrada);
        if (motivoRechazo != null) {
            throw rechazar(motivoRechazo, idEvento, idPersonal);
        }

        Instant ahora = Instant.now();
        if (entradaRepository.marcarUsada(qr, idEvento, ahora, idPersonal) != 1) {
            throw rechazar(QrError.QR_YA_USADO, idEvento, idPersonal);
        }
        // El UPDATE limpia el contexto de JPA; la entidad leída queda separada y solo se usa para la respuesta.
        entrada.setEstado("USADA");
        entrada.setIngresadaEn(ahora);
        entrada.setIdPersonalAcceso(idPersonal);
        log.info("ingreso_aprobado idEntrada={} idEvento={} idPersonal={} fecha={}",
                entrada.getId(), idEvento, idPersonal, ahora);
        return convertirRespuesta(entrada);
    }

    private QrError motivoRechazo(Entrada entrada) {
        if (entrada.getEstado() == null) {
            return null;
        }
        return switch (entrada.getEstado()) {
            case "USADA" -> QrError.QR_YA_USADO;
            case "EN_REINTEGRO" -> "DEVOLUCION".equals(entrada.getMotivoInvalidacion())
                    ? QrError.QR_DEVUELTO
                    : QrError.QR_EVENTO_CANCELADO;
            case "VENCIDA", "CANCELADA" -> QrError.QR_VENCIDO;
            default -> null;
        };
    }

    private QrRechazadoException rechazar(QrError error, Long idEvento, Long idPersonal) {
        log.warn("ingreso_rechazado idEvento={} idPersonal={} code={}", idEvento, idPersonal, error.name());
        return new QrRechazadoException(error);
    }

    @Transactional
    public java.util.Map<String, Object> disponibilidad(Long idEvento, Long idSector) {
        entradaRepository.vencerReservas(Instant.now());
        SectorResponse sector = sectorClient.obtenerPorId(idSector);
        validarRespuestaRemota(sector == null || !idSector.equals(sector.idSector())
                        || sector.capacidadMax() == null || sector.capacidadMax() <= 0,
                "Ubicación no devolvió una capacidad válida para el sector");
        Integer capacidad = sector.capacidadMax();
        long ocupadas = entradaRepository.contarActivasDeSector(idEvento, idSector, Instant.now());
        return java.util.Map.of(
                "idEvento", idEvento,
                "idSector", idSector,
                "capacidad", capacidad,
                "ocupadas", ocupadas,
                "disponibles", Math.max(0, capacidad - ocupadas)
        );
    }

    private Entrada buscarEntidadPorId(Long id) {
        return entradaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entrada con id " + id));
    }

    private void aplicarDatos(Entrada entrada, EntradaRequest request) {
        entrada.setIdEvento(request.getIdEvento());
        entrada.setIdUsuario(request.getIdUsuario());
        entrada.setIdSector(request.getIdSector());
        entrada.setFila(request.getFila());
        entrada.setAsiento(request.getAsiento());
        if (entrada.getEstado() == null) {
            entrada.setCodigoQr(null);
        }
    }

    private EntradaResponse convertirRespuesta(Entrada entrada) {
        return new EntradaResponse(
                entrada.getId(),
                entrada.getIdEvento(),
                entrada.getIdUsuario(),
                entrada.getIdSector(),
                entrada.getFila(),
                entrada.getAsiento(),
                entrada.getPrecio(),
                entrada.getCodigoQr(),
                entrada.getEstado(),
                entrada.getExpiraEn()
        );
    }

    private void validarReserva(
            EntradaRequest request,
            EventoResponse evento,
            SectorResponse sector
    ) {
        if (sector.capacidadMax() == null || sector.capacidadMax() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Ubicación devolvió una capacidad inválida para el sector"
            );
        }
        if (evento.fechaHora() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Eventos no devolvió la fecha del evento"
            );
        }
        if (!evento.fechaHora().isAfter(Instant.now())) {
            throw new IllegalArgumentException("La fecha del evento debe ser futura");
        }
        if (!"PUBLICADO".equalsIgnoreCase(evento.estado())) {
            throw new IllegalStateException("Solo se pueden reservar entradas para eventos publicados");
        }
        if ("NUMERADO".equalsIgnoreCase(sector.tipo())) {
            if (request.getFila() == null || request.getAsiento() == null) {
                throw new IllegalArgumentException("Debe indicar fila y asiento para un sector numerado");
            }
            if (request.getFila() <= 0 || request.getAsiento() <= 0) {
                throw new IllegalArgumentException("Fila y asiento deben ser positivos");
            }
            if (sector.filas() == null || sector.filas() <= 0
                    || sector.asientosPorFila() == null || sector.asientosPorFila() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ubicación devolvió dimensiones inválidas para el sector numerado"
                );
            }
            if (request.getFila() > sector.filas() || request.getAsiento() > sector.asientosPorFila()) {
                throw new IllegalArgumentException("El asiento no coincide con la capacidad numerada del sector");
            }
        } else if ("NO_NUMERADO".equalsIgnoreCase(sector.tipo())) {
            if (request.getFila() != null || request.getAsiento() != null) {
                throw new IllegalArgumentException("No se puede indicar fila ni asiento para un sector no numerado");
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Ubicación devolvió un tipo de sector inválido"
            );
        }
    }

    private void validarClaveIdempotencia(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key es obligatorio y admite hasta 100 caracteres");
        }
    }

    private void validarRespuestaRemota(boolean invalida, String mensaje) {
        if (invalida) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, mensaje);
        }
    }

}
