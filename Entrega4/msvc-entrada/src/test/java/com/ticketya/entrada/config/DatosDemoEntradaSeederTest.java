package com.ticketya.entrada.config;

import com.ticketya.entrada.entity.Compra;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.FilaEvento;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.repository.CompraRepository;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.FilaEventoRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatosDemoEntradaSeederTest {

    private FilaEventoRepository filas;
    private CompraRepository compras;
    private EntradaRepository entradas;
    private PagoRepository pagos;
    private RegistroPagoRepository registros;
    private DatosDemoEntradaSeeder seeder;

    @BeforeEach
    void setUp() {
        filas = mock(FilaEventoRepository.class);
        compras = mock(CompraRepository.class);
        entradas = mock(EntradaRepository.class);
        pagos = mock(PagoRepository.class);
        registros = mock(RegistroPagoRepository.class);
        AtomicLong ids = new AtomicLong(100);
        when(compras.save(any(Compra.class))).thenAnswer(i -> {
            Compra c = i.getArgument(0);
            c.setIdCompra(ids.incrementAndGet());
            return c;
        });
        when(pagos.save(any(Pago.class))).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setIdPago(ids.incrementAndGet());
            return p;
        });
        when(entradas.save(any(Entrada.class))).thenAnswer(i -> {
            Entrada e = i.getArgument(0);
            e.setId(ids.incrementAndGet());
            return e;
        });
        seeder = new DatosDemoEntradaSeeder(filas, compras, entradas, pagos, registros, 3L);
    }

    @Test
    void creaUnaFilaActivaPorEventoPublicadoYDosComprasPagadasDelCliente() {
        seeder.run(null);

        ArgumentCaptor<FilaEvento> fila = ArgumentCaptor.forClass(FilaEvento.class);
        verify(filas, times(18)).save(fila.capture());
        assertTrue(fila.getAllValues().stream().allMatch(f -> "ACTIVA".equals(f.getEstado())));
        assertEquals(5400, fila.getAllValues().getFirst().getCapacidadTotal());

        ArgumentCaptor<Compra> compra = ArgumentCaptor.forClass(Compra.class);
        verify(compras, times(2)).save(compra.capture());
        assertTrue(compra.getAllValues().stream().allMatch(c -> Compra.PAGADA.equals(c.getEstado()) && c.getIdUsuario() == 3L));

        ArgumentCaptor<Entrada> entrada = ArgumentCaptor.forClass(Entrada.class);
        verify(entradas, times(3)).save(entrada.capture());
        assertTrue(entrada.getAllValues().stream().allMatch(e -> "VALIDA".equals(e.getEstado()) && e.getCodigoQr() != null));
        verify(registros, times(3)).save(any());
    }

    @Test
    void siYaHayDatosNoHaceNada() {
        when(compras.count()).thenReturn(1L);

        seeder.run(null);

        verify(filas, never()).save(any());
        verify(compras, never()).save(any());
    }
}
