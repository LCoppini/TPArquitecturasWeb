package com.ticketya.notificaciones.service.impl;

import com.ticketya.notificaciones.config.CorrelationIdFilter;
import com.ticketya.notificaciones.dto.request.SolicitudNotificacionRequest;
import com.ticketya.notificaciones.dto.response.NotificacionResponse;
import com.ticketya.notificaciones.entity.EstadoNotificacion;
import com.ticketya.notificaciones.entity.Notificacion;
import com.ticketya.notificaciones.exception.RecursoNoEncontradoException;
import com.ticketya.notificaciones.exception.SolicitudInvalidaException;
import com.ticketya.notificaciones.mapper.NotificacionMapper;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import com.ticketya.notificaciones.service.NotificacionEnvioService;
import com.ticketya.notificaciones.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sin @Transactional a nivel de clase a proposito: la insercion debe confirmarse ANTES de disparar el envio
 * asincrono, o el hilo del pool podria no ver la fila.
 */
@Service
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository repository;
    private final NotificacionMapper mapper;
    private final PlantillaEmail plantilla;
    private final NotificacionEnvioService envioService;

    public NotificacionServiceImpl(NotificacionRepository repository,
                                   NotificacionMapper mapper,
                                   PlantillaEmail plantilla,
                                   NotificacionEnvioService envioService) {
        this.repository = repository;
        this.mapper = mapper;
        this.plantilla = plantilla;
        this.envioService = envioService;
    }

    @Override
    public NotificacionResponse solicitar(SolicitudNotificacionRequest request) {
        String clave = request.claveIdempotencia().trim();
        Map<String, String> datos = request.datos() == null ? Map.of() : request.datos();

        List<String> faltantes = plantilla.faltantes(request.tipo(), datos);
        if (!faltantes.isEmpty()) {
            throw new SolicitudInvalidaException("Faltan datos obligatorios para " + request.tipo()
                    + ": " + String.join(", ", faltantes));
        }

        Optional<Notificacion> existente = repository.findByClaveIdempotencia(clave);
        if (existente.isPresent()) {
            return mapper.aResponse(existente.get());
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setClaveIdempotencia(clave);
        notificacion.setIdUsuario(request.idUsuario());
        notificacion.setTipo(request.tipo());
        notificacion.setDestinatario(request.destinatario() == null || request.destinatario().isBlank()
                ? null
                : request.destinatario().trim());
        notificacion.setDatos(new HashMap<>(datos));
        notificacion.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));

        Notificacion guardada;
        try {
            guardada = repository.save(notificacion);
        } catch (DataIntegrityViolationException e) {
            // Otra solicitud con la misma clave se adelanto: se devuelve esa.
            return repository.findByClaveIdempotencia(clave)
                    .map(mapper::aResponse)
                    .orElseThrow(() -> e);
        }

        despachar(guardada.getId());
        return mapper.aResponse(guardada);
    }

    @Override
    public NotificacionResponse obtenerPorId(Long id) {
        return repository.findById(id)
                .map(mapper::aResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificacion " + id + " no existe"));
    }

    @Override
    public List<NotificacionResponse> listar(Long idUsuario, EstadoNotificacion estado) {
        List<Notificacion> notificaciones;
        if (idUsuario != null && estado != null) {
            notificaciones = repository.findTop100ByIdUsuarioAndEstadoOrderByIdDesc(idUsuario, estado);
        } else if (idUsuario != null) {
            notificaciones = repository.findTop100ByIdUsuarioOrderByIdDesc(idUsuario);
        } else if (estado != null) {
            notificaciones = repository.findTop100ByEstadoOrderByIdDesc(estado);
        } else {
            notificaciones = repository.findTop100ByOrderByIdDesc();
        }
        return notificaciones.stream().map(mapper::aResponse).toList();
    }

    private void despachar(Long id) {
        try {
            envioService.enviarAsync(id);
        } catch (TaskRejectedException e) {
            // Pool saturado: la notificacion queda PENDIENTE y la toma el worker de reintentos.
            log.warn("Pool de envios saturado: la notificacion {} queda PENDIENTE para el worker", id);
        }
    }
}
