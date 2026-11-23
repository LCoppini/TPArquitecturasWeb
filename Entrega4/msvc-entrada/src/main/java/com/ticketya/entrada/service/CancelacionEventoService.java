package com.ticketya.entrada.service;

import org.springframework.stereotype.Service;
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

    @Transactional
    public void cancelarEvento(Long idEvento) {
        // Ambos servicios usan la misma base y se unen a esta transacción.
        filaVirtualService.cancelarEvento(idEvento);
        entradaService.cancelarEvento(idEvento);
        pagoService.cancelarEvento(idEvento);
    }
}
