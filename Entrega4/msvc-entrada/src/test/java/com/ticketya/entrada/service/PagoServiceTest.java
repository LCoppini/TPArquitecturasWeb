package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private RegistroPagoRepository registroPagoRepository;

    @Test
    void noPermitePagarUnaReservaCancelada() {
        Entrada entrada = entrada(100L, "CANCELADA");
        when(entradaRepository.buscarPorIdConBloqueo(100L)).thenReturn(Optional.of(entrada));
        when(pagoRepository.findByIdEntradaAndClaveIdempotencia(100L, "pago-1"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> servicio("APRUEBA").crear(solicitud()));

        verify(pagoRepository, never()).save(org.mockito.ArgumentMatchers.any(Pago.class));
    }

    @Test
    void dejaReintegroPendienteSiFallaYCompletaElReintentoUnaSolaVez() {
        Entrada entrada = entrada(100L, "EN_REINTEGRO");
        Pago pago = pago(200L, 100L, "APROBADO");
        when(entradaRepository.findByIdEventoAndEstadoIn(20L, List.of("EN_REINTEGRO")))
                .thenAnswer(invocacion -> "EN_REINTEGRO".equals(entrada.getEstado())
                        ? List.of(entrada)
                        : List.of());
        when(pagoRepository.findByIdEntradaIn(List.of(100L))).thenReturn(List.of(pago));

        servicio("FALLA").cancelarEvento(20L);

        assertEquals("REINTEGRO_PENDIENTE", pago.getEstado());
        assertEquals("EN_REINTEGRO", entrada.getEstado());

        servicio("APRUEBA").cancelarEvento(20L);

        assertEquals("REEMBOLSADO", pago.getEstado());
        assertEquals("DEVUELTA", entrada.getEstado());
        String referenciaReintegro = pago.getReferenciaPasarela();
        assertFalse(referenciaReintegro == null || referenciaReintegro.isBlank());

        servicio("APRUEBA").cancelarEvento(20L);

        assertEquals(referenciaReintegro, pago.getReferenciaPasarela());
        verify(pagoRepository, times(2)).findByIdEntradaIn(List.of(100L));
    }

    @Test
    void noCambiaEntradasSiNoEncuentraUnPagoReintegrable() {
        Entrada entrada = entrada(100L, "EN_REINTEGRO");
        Pago pago = pago(200L, 100L, "PENDIENTE");
        when(entradaRepository.findByIdEventoAndEstadoIn(20L, List.of("EN_REINTEGRO")))
                .thenReturn(List.of(entrada));
        when(pagoRepository.findByIdEntradaIn(List.of(100L))).thenReturn(List.of(pago));

        servicio("APRUEBA").cancelarEvento(20L);

        assertEquals("PENDIENTE", pago.getEstado());
        assertEquals("EN_REINTEGRO", entrada.getEstado());
    }

    private PagoService servicio(String resultadoPasarela) {
        return new PagoService(pagoRepository, entradaRepository, registroPagoRepository, resultadoPasarela);
    }

    private PagoRequest solicitud() {
        PagoRequest request = new PagoRequest();
        request.setIdEntrada(100L);
        request.setIdUsuario(10L);
        request.setMedioDePago("TARJETA");
        request.setClaveIdempotencia("pago-1");
        return request;
    }

    private Entrada entrada(Long id, String estado) {
        Entrada entrada = new Entrada();
        entrada.setId(id);
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setEstado(estado);
        return entrada;
    }

    private Pago pago(Long id, Long idEntrada, String estado) {
        Pago pago = new Pago();
        pago.setIdPago(id);
        pago.setIdEntrada(idEntrada);
        pago.setIdUsuario(10L);
        pago.setEstado(estado);
        pago.setMedioDePago("TARJETA");
        return pago;
    }
}
