package com.ticketya.entrada.service;

import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.event.EventoModificadoEvent;
import com.ticketya.entrada.repository.EntradaRepository;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Recibe de msvc-evento un cambio ya aplicado y avisa a quienes tienen entradas vigentes del evento. */
@Service
public class ModificacionEventoService {

    private final EntradaRepository entradaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ModificacionEventoService(EntradaRepository entradaRepository, ApplicationEventPublisher eventPublisher) {
        this.entradaRepository = entradaRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Devuelve la cantidad de usuarios a avisar. */
    @Transactional
    public int notificarModificacion(Long idEvento, String cambios) {
        List<Long> afectados = entradaRepository.buscarUsuariosConEntradasValidas(idEvento);
        if (!afectados.isEmpty()) {
            eventPublisher.publishEvent(new EventoModificadoEvent(idEvento, afectados, cambios, correlacion()));
        }
        return afectados.size();
    }

    private String correlacion() {
        String correlationId = MDC.get(CorrelationContext.MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "no-disponible" : correlationId;
    }
}
