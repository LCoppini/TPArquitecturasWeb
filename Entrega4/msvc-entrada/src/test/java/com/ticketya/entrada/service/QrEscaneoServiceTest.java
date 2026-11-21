package com.ticketya.entrada.service;

import com.ticketya.entrada.client.EventoClient;
import com.ticketya.entrada.client.SectorClient;
import com.ticketya.entrada.client.UsuarioClient;
import com.ticketya.entrada.dto.response.EntradaResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.exception.QrError;
import com.ticketya.entrada.exception.QrRechazadoException;
import com.ticketya.entrada.repository.EntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrEscaneoServiceTest {

    @Mock
    private EntradaRepository entradaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private EventoClient eventoClient;

    @Mock
    private SectorClient sectorClient;

    private EntradaService entradaService;

    @BeforeEach
    void setUp() {
        entradaService = new EntradaService(entradaRepository, usuarioClient, eventoClient, sectorClient, 6);
    }

    @Test
    void escaneoValidoMarcaUsadaYRegistraAlPersonal() {
        Entrada entrada = entrada("VALIDA", null);
        when(entradaRepository.findByCodigoQr("qr-1")).thenReturn(Optional.of(entrada));
        when(entradaRepository.marcarUsada(eq("qr-1"), eq(20L), any(Instant.class), eq(99L))).thenReturn(1);

        EntradaResponse respuesta = entradaService.validarQr("qr-1", 20L, 99L);

        assertEquals("USADA", respuesta.getEstado());
        assertEquals("USADA", entrada.getEstado());
        assertEquals(99L, entrada.getIdPersonalAcceso());
        assertNotNull(entrada.getIngresadaEn());
        ArgumentCaptor<Instant> fecha = ArgumentCaptor.forClass(Instant.class);
        verify(entradaRepository).marcarUsada(eq("qr-1"), eq(20L), fecha.capture(), eq(99L));
        assertNotNull(fecha.getValue());
    }

    @Test
    void qrInexistenteSeRechazaCon404() {
        when(entradaRepository.findByCodigoQr("qr-inexistente")).thenReturn(Optional.empty());

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-inexistente", 20L, 99L));

        assertEquals(QrError.QR_NO_EXISTE, error.getError());
        verify(entradaRepository, never()).marcarUsada(any(), any(), any(), any());
    }

    @Test
    void qrDeOtroEventoSeRechaza() {
        Entrada entrada = entrada("VALIDA", null);
        entrada.setIdEvento(21L);
        when(entradaRepository.findByCodigoQr("qr-1")).thenReturn(Optional.of(entrada));

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-1", 20L, 99L));

        assertEquals(QrError.QR_EVENTO_DISTINTO, error.getError());
        verify(entradaRepository, never()).marcarUsada(any(), any(), any(), any());
    }

    @Test
    void entradaDevueltaSeRechazaConSuMotivo() {
        when(entradaRepository.findByCodigoQr("qr-1"))
                .thenReturn(Optional.of(entrada("EN_REINTEGRO", "DEVOLUCION")));

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-1", 20L, 99L));

        assertEquals(QrError.QR_DEVUELTO, error.getError());
    }

    @Test
    void eventoCanceladoSeRechazaConSuMotivo() {
        when(entradaRepository.findByCodigoQr("qr-1"))
                .thenReturn(Optional.of(entrada("EN_REINTEGRO", "CANCELACION")));

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-1", 20L, 99L));

        assertEquals(QrError.QR_EVENTO_CANCELADO, error.getError());
    }

    @Test
    void reservaVencidaSeRechaza() {
        when(entradaRepository.findByCodigoQr("qr-1"))
                .thenReturn(Optional.of(entrada("VENCIDA", null)));

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-1", 20L, 99L));

        assertEquals(QrError.QR_VENCIDO, error.getError());
    }

    @Test
    void qrYaUsadoSeRechaza() {
        when(entradaRepository.findByCodigoQr("qr-1"))
                .thenReturn(Optional.of(entrada("USADA", null)));

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-1", 20L, 99L));

        assertEquals(QrError.QR_YA_USADO, error.getError());
    }

    @Test
    void reintentoTrasPerderLaCarreraSeRechazaSinCambiarLaEntrada() {
        Entrada entrada = entrada("VALIDA", null);
        when(entradaRepository.findByCodigoQr("qr-1")).thenReturn(Optional.of(entrada));
        when(entradaRepository.marcarUsada(eq("qr-1"), eq(20L), any(Instant.class), eq(99L))).thenReturn(0);

        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("qr-1", 20L, 99L));

        assertEquals(QrError.QR_YA_USADO, error.getError());
        assertEquals("VALIDA", entrada.getEstado());
        assertEquals(null, entrada.getIdPersonalAcceso());
    }

    @Test
    void qrEnBlancoSeRechazaSinConsultarLaBase() {
        QrRechazadoException error = assertThrows(QrRechazadoException.class,
                () -> entradaService.validarQr("   ", 20L, 99L));

        assertEquals(QrError.QR_REQUEST_INVALIDO, error.getError());
        verify(entradaRepository, never()).findByCodigoQr(any());
    }

    @Test
    void devolucionAprobadaEscribeElMotivo() {
        Entrada entrada = entrada("VALIDA", null);
        entrada.setEstadoDevolucion("PENDIENTE");
        when(entradaRepository.findById(1L)).thenReturn(Optional.of(entrada));

        entradaService.resolverDevolucion(1L, true);

        assertEquals("EN_REINTEGRO", entrada.getEstado());
        assertEquals("DEVOLUCION", entrada.getMotivoInvalidacion());
    }

    private Entrada entrada(String estado, String motivo) {
        Entrada entrada = new Entrada();
        entrada.setId(1L);
        entrada.setIdEvento(20L);
        entrada.setIdUsuario(10L);
        entrada.setIdSector(30L);
        entrada.setPrecio(BigDecimal.TEN);
        entrada.setCodigoQr("qr-1");
        entrada.setEstado(estado);
        entrada.setMotivoInvalidacion(motivo);
        return entrada;
    }
}
