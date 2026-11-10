package com.ticketya.entrada.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelacionEventoService {

    private final EntradaService entradaService;
    private final PagoService pagoService;

    public CancelacionEventoService(EntradaService entradaService, PagoService pagoService) {
        this.entradaService = entradaService;
        this.pagoService = pagoService;
    }

    @Transactional
    public void cancelarEvento(Long idEvento) {
        // Ambos servicios usan la misma base y se unen a esta transacción.
        entradaService.cancelarEvento(idEvento);
        pagoService.cancelarEvento(idEvento);
    }
}
