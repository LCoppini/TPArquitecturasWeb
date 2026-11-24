package com.ticketya.entrada.service;

import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.event.EventoCanceladoEvent;
import com.ticketya.entrada.repository.EntradaRepository;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelacionEventoService {

    private final EntradaService entradaService;
    private final PagoService pagoService;
    private final FilaVirtualService filaVirtualService;

    public CancelacionEventoService(
            EntradaService entradaService,
            PagoService pagoService,
            FilaVirtualService filaVirtualService
    ) {
        this.entradaService = entradaService;
        this.pagoService = pagoService;
        this.filaVirtualService = filaVirtualService;
    }

    private EntradaRepository entradaRepository;
    private ApplicationEventPublisher eventPublisher;

    /** Opcionales: sin ellos la cancelacion funciona igual, solo que no se avisa a los clientes. */
    @Autowired(required = false)
    public void setNotificacion(EntradaRepository entradaRepository, ApplicationEventPublisher eventPublisher) {
        this.entradaRepository = entradaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void cancelarEvento(Long idEvento) {
        List<Long> afectados = entradaRepository == null ? List.of()
                : entradaRepository.buscarUsuariosConEntradasValidas(idEvento);
        // Ambos servicios usan la misma base y se unen a esta transacción.
        filaVirtualService.cancelarEvento(idEvento);
        entradaService.cancelarEvento(idEvento);
        pagoService.cancelarEvento(idEvento);
        if (eventPublisher != null && !afectados.isEmpty()) {
            eventPublisher.publishEvent(new EventoCanceladoEvent(idEvento, afectados, correlationId()));
        }
    }

    private String correlationId() {
        String correlationId = MDC.get(CorrelationContext.MDC_KEY);
        return correlationId == null || correlationId.isBlank() ? "no-disponible" : correlationId;
    }
}
