package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.TandaFilaRepository;
import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.service.CompraService;
import com.ticketya.entrada.service.FilaVirtualService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class FilaVirtualScheduler {

    private static final Logger log = LoggerFactory.getLogger(FilaVirtualScheduler.class);

    private final TandaFilaRepository tandaFilaRepository;
    private final CompraRepository compraRepository;
    private final FilaVirtualService filaVirtualService;
    private final CompraService compraService;

    @Autowired
    public FilaVirtualScheduler(
            TandaFilaRepository tandaFilaRepository,
            CompraRepository compraRepository,
            FilaVirtualService filaVirtualService,
            CompraService compraService
    ) {
        this.tandaFilaRepository = tandaFilaRepository;
        this.compraRepository = compraRepository;
        this.filaVirtualService = filaVirtualService;
        this.compraService = compraService;
    }

    public FilaVirtualScheduler(
            TandaFilaRepository tandaFilaRepository,
            FilaVirtualService filaVirtualService
    ) {
        this(tandaFilaRepository, null, filaVirtualService, null);
    }

    @Scheduled(fixedDelayString = "${ticketya.fila-virtual.scheduler-ms:5000}")
    public void cerrarTandasVencidas() {
        Instant ahora = Instant.now();
        java.util.stream.Stream<Long> eventosTanda = tandaFilaRepository.buscarEventosConTandaVencida(ahora).stream();
        java.util.stream.Stream<Long> eventosCompra = compraRepository == null
                ? java.util.stream.Stream.empty()
                : compraRepository.buscarEventosConCompraVencida(ahora).stream();
        java.util.stream.Stream.concat(eventosTanda, eventosCompra).distinct()
                .forEach(this::cerrarTandaVencida);
    }

    private void cerrarTandaVencida(Long idEvento) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CorrelationContext.MDC_KEY, correlationId);
        try {
            if (compraService != null) {
                compraService.expirarComprasVencidas(idEvento, Instant.now());
            }
            filaVirtualService.cerrarTandaVencida(idEvento);
        } catch (RuntimeException exception) {
            log.error("No se pudo cerrar la tanda vencida del evento {} correlationId={}",
                    idEvento, correlationId, exception);
        } finally {
            MDC.remove(CorrelationContext.MDC_KEY);
        }
    }
}
