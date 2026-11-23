package com.ticketya.entrada.service;

import com.ticketya.entrada.config.CorrelationContext;
import com.ticketya.entrada.dto.request.PagoRequest;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.PendientePreferencia;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.PendientePreferenciaRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServicePendienteTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private FilaEventoRepository filaEventoRepository;

    @Mock
    private RegistroPagoRepository registroPagoRepository;

    @Mock
    private PendientePreferenciaRepository pendienteRepository;

    @BeforeEach
    void ponerCorrelationId() {
        MDC.put(CorrelationContext.MDC_KEY, "corr-pago-1");
    }

    @AfterEach
    void limpiarCorrelationId() {
        MDC.clear();
    }

    @Test
    void pagoAprobadoCreaUnPendienteConElCorrelationId() {
        prepararReserva(Instant.now().plusSeconds(600));
        when(pendienteRepository.findByIdEntrada(100L)).thenReturn(Optional.empty());

        servicio("APRUEBA").crear(solicitud());

        ArgumentCaptor<PendientePreferencia> captor = ArgumentCaptor.forClass(PendientePreferencia.class);
        verify(pendienteRepository).save(captor.capture());
        PendientePreferencia pendiente = captor.getValue();
        assertEquals(100L, pendiente.getIdEntrada());
        assertEquals(10L, pendiente.getIdUsuario());
        assertEquals(20L, pendiente.getIdEvento());
        assertEquals("corr-pago-1", pendiente.getCorrelationId());
        assertEquals(PendientePreferencia.PENDIENTE, pendiente.getEstado());
        assertEquals(0, pendiente.getIntentos());
    }

    @Test
    void pagoRechazadoNoCreaPendiente() {
        prepararReserva(Instant.now().plusSeconds(600));

        servicio("RECHAZA").crear(solicitud());

        verify(pendienteRepository, never()).save(any());
    }

    @Test
    void reservaVencidaNoCreaPendiente() {
        prepararReserva(Instant.now().minusSeconds(60));

        assertThrows(IllegalStateException.class, () -> servicio("APRUEBA").crear(solicitud()));

        verify(pendienteRepository, never()).save(any());
    }

    @Test
    void unPendienteYaRegistradoNoSeDuplica() {
        prepararReserva(Instant.now().plusSeconds(600));
        when(pendienteRepository.findByIdEntrada(100L))
                .thenReturn(Optional.of(new PendientePreferencia()));

        servicio("APRUEBA").crear(solicitud());

        verify(pendienteRepository, never()).save(any());
    }

    private void prepararReserva(Instant expiraEn) {
        Entrada entrada = new Entrada();
        entrada.setId(100L);
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setEstado("RESERVADA");
        entrada.setExpiraEn(expiraEn);
        when(entradaRepository.buscarPorIdConBloqueo(100L)).thenReturn(Optional.of(entrada));
        when(entradaRepository.findById(100L)).thenReturn(Optional.of(entrada));
        when(pagoRepository.findByIdEntradaAndClaveIdempotencia(100L, "pago-1"))
                .thenReturn(Optional.empty());
        lenient().when(pagoRepository.save(any(Pago.class))).thenAnswer(invocacion -> {
            Pago pago = invocacion.getArgument(0);
            pago.setIdPago(200L);
            return pago;
        });
    }

    private PagoService servicio(String resultadoPasarela) {
        return new PagoService(pagoRepository, entradaRepository, filaEventoRepository,
                registroPagoRepository, pendienteRepository, resultadoPasarela);
    }

    private PagoRequest solicitud() {
        PagoRequest request = new PagoRequest();
        request.setIdEntrada(100L);
        request.setIdUsuario(10L);
        request.setMedioDePago("TARJETA");
        request.setClaveIdempotencia("pago-1");
        return request;
    }
}
