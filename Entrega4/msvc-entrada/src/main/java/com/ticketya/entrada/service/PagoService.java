package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.dto.response.PagoResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.RegistroPago;
import com.ticketya.entrada.entity.RegistroPagoId;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PagoService {

    private final PagoRepository pagoRepository;
    private final EntradaRepository entradaRepository;
    private final RegistroPagoRepository registroPagoRepository;
    private final String resultadoPasarela;

    public PagoService(
            PagoRepository pagoRepository,
            EntradaRepository entradaRepository,
            RegistroPagoRepository registroPagoRepository,
            @Value("${ticketya.pasarela.resultado:APRUEBA}") String resultadoPasarela
    ) {
        this.pagoRepository = pagoRepository;
        this.entradaRepository = entradaRepository;
        this.registroPagoRepository = registroPagoRepository;
        this.resultadoPasarela = resultadoPasarela.toUpperCase(java.util.Locale.ROOT);
    }

    @Transactional
    public PagoResponse crear(PagoRequest request) {
        validarClave(request.getClaveIdempotencia());
        Entrada entrada = entradaRepository.buscarPorIdConBloqueo(request.getIdEntrada())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró la reserva " + request.getIdEntrada()
                ));

        String huella = huella(request);
        Pago existente = pagoRepository.findByIdEntradaAndClaveIdempotencia(
                request.getIdEntrada(), request.getClaveIdempotencia()
        ).orElse(null);
        if (existente != null) {
            if (!huella.equals(existente.getHuellaSolicitud())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                        "La clave de idempotencia ya se usó con otros datos"
                );
            }
            return convertirRespuesta(existente);
        }

        if ("RESERVADA".equals(entrada.getEstado())
                && entrada.getExpiraEn() != null
                && !entrada.getExpiraEn().isAfter(Instant.now())) {
            entrada.setEstado("VENCIDA");
            entrada.setExpiraEn(null);
        }
        if (!entrada.getIdUsuario().equals(request.getIdUsuario())) {
            throw new IllegalStateException("El cliente solo puede pagar sus propias reservas");
        }
        if (!"RESERVADA".equals(entrada.getEstado())
                || entrada.getExpiraEn() == null
                || !entrada.getExpiraEn().isAfter(Instant.now())) {
            throw new IllegalStateException("La reserva venció o ya no está disponible");
        }

        Pago pago = new Pago();
        pago.setIdUsuario(request.getIdUsuario());
        pago.setIdEntrada(request.getIdEntrada());
        pago.setClaveIdempotencia(request.getClaveIdempotencia());
        pago.setHuellaSolicitud(huella);
        pago.setMedioDePago(request.getMedioDePago());
        pago.setEstado("PENDIENTE");
        pago = pagoRepository.save(pago);

        if ("FALLA".equals(resultadoPasarela)) {
            return convertirRespuesta(pago);
        }
        if (!"APRUEBA".equals(resultadoPasarela) && !"RECHAZA".equals(resultadoPasarela)
                && !"DEMORA".equals(resultadoPasarela)) {
            throw new IllegalStateException("Configuración PASARELA_RESULTADO inválida");
        }

        if ("RECHAZA".equals(resultadoPasarela)) {
            pago.setEstado("RECHAZADO");
            entrada.setEstado("VENCIDA");
            entrada.setExpiraEn(null);
            return convertirRespuesta(pago);
        }
        if (!entrada.getExpiraEn().isAfter(Instant.now())) {
            pago.setEstado("REINTEGRO_PENDIENTE");
            pago.setReferenciaPasarela("mock-" + UUID.randomUUID());
            entrada.setEstado("VENCIDA");
            entrada.setExpiraEn(null);
            return convertirRespuesta(pago);
        }

        pago.setEstado("APROBADO");
        pago.setReferenciaPasarela("mock-" + UUID.randomUUID());
        entrada.setEstado("VALIDA");
        entrada.setExpiraEn(null);
        entrada.setCodigoQr(UUID.randomUUID().toString());

        RegistroPagoId id = new RegistroPagoId();
        id.setIdEntrada(entrada.getId());
        id.setIdPago(pago.getIdPago());
        RegistroPago registro = new RegistroPago();
        registro.setId(id);
        registro.setEntrada(entrada);
        registro.setPago(pago);
        registro.setFechaPago(LocalDateTime.now(ZoneOffset.UTC));
        registro.setDescripcion("Pago de entrada " + entrada.getId() + " con " + pago.getMedioDePago());
        registro.setMontoTotal(entrada.getPrecio());
        registroPagoRepository.save(registro);
        return convertirRespuesta(pago);
    }

    public List<PagoResponse> listar() {
        return pagoRepository.findAll().stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    public List<PagoResponse> listarDeUsuario(Long idUsuario) {
        return pagoRepository.findByIdUsuarioOrderByIdPagoDesc(idUsuario).stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    public PagoResponse buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .map(this::convertirRespuesta)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el pago con id " + id));
    }

    public PagoResponse buscarPorIdDeUsuario(Long id, Long idUsuario) {
        return pagoRepository.findByIdPagoAndIdUsuario(id, idUsuario)
                .map(this::convertirRespuesta)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el pago con id " + id));
    }

    @Transactional
    public void reintegrarEntrada(Long idEntrada) {
        Entrada entrada = entradaRepository.findById(idEntrada)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la entrada con id " + idEntrada));
        if (!"EN_REINTEGRO".equals(entrada.getEstado())) {
            throw new IllegalStateException("La entrada no está pendiente de reintegro");
        }
        Pago pago = pagoRepository.findFirstByIdEntradaOrderByIdPagoDesc(entrada.getId())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el pago de la entrada " + idEntrada));
        if ("FALLA".equals(resultadoPasarela)) {
            pago.setEstado("REINTEGRO_PENDIENTE");
            return;
        }
        pago.setEstado("REEMBOLSADO");
        pago.setReferenciaPasarela("refund-" + UUID.randomUUID());
        entrada.setEstado("DEVUELTA");
    }

    @Transactional
    public void cancelarEvento(Long idEvento) {
        List<Entrada> entradas = entradaRepository.findByIdEventoAndEstadoIn(
                idEvento,
                List.of("EN_REINTEGRO")
        );
        if (entradas.isEmpty()) {
            return;
        }
        // Los IDs de entrada ya son únicos y el repositorio acepta una colección; no hace falta convertir ni castear.
        List<Long> idsEntradas = entradas.stream()
                .map(Entrada::getId)
                .toList();
        List<Pago> pagos = pagoRepository.findByIdEntradaIn(idsEntradas);
        Set<Long> entradasReintegradas = new HashSet<>();
        for (Pago pago : pagos) {
            if ("REEMBOLSADO".equals(pago.getEstado())) {
                entradasReintegradas.add(pago.getIdEntrada());
                continue;
            }
            // Solo se reintegran cobros aprobados o previamente pendientes de reintegro.
            if (!"APROBADO".equals(pago.getEstado()) && !"REINTEGRO_PENDIENTE".equals(pago.getEstado())) {
                continue;
            }
            if ("FALLA".equals(resultadoPasarela)) {
                pago.setEstado("REINTEGRO_PENDIENTE");
            } else {
                pago.setEstado("REEMBOLSADO");
                pago.setReferenciaPasarela("refund-" + UUID.randomUUID());
                entradasReintegradas.add(pago.getIdEntrada());
            }
        }
        // No cerrar entradas si no tienen un pago reintegrado o el reintegro sigue fallando.
        entradas.stream()
                .filter(entrada -> entradasReintegradas.contains(entrada.getId()))
                .forEach(entrada -> entrada.setEstado("DEVUELTA"));
    }

    @Transactional
    public PagoResponse actualizar(Long id, PagoRequest datos) {
        throw new IllegalStateException("Los pagos no se pueden editar por CRUD");
    }

    @Transactional
    public void eliminar(Long id) {
        throw new IllegalStateException("Los pagos no se pueden eliminar por CRUD");
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 100) {
            throw new IllegalArgumentException("La clave de idempotencia es obligatoria y admite hasta 100 caracteres");
        }
    }

    private String huella(PagoRequest request) {
        String contenido = request.getIdEntrada() + "|" + request.getIdUsuario() + "|"
                + request.getMedioDePago().trim().toLowerCase(java.util.Locale.ROOT);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private PagoResponse convertirRespuesta(Pago pago) {
        return new PagoResponse(
                pago.getIdPago(),
                pago.getIdUsuario(),
                pago.getEstado(),
                pago.getMedioDePago(),
                pago.getIdEntrada(),
                pago.getReferenciaPasarela()
        );
    }
}
