package com.ticketya.preferencias.service;

import com.ticketya.preferencias.dto.response.ClientesDeArtistaResponse;
import com.ticketya.preferencias.repository.PreferenciaClienteRepository;
import com.ticketya.preferencias.service.impl.PreferenciaServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenciaServiceClientesPorArtistaTest {

    private final PreferenciaClienteRepository repository = mock(PreferenciaClienteRepository.class);
    private final PreferenciaService service = new PreferenciaServiceImpl(repository);

    @Test
    void consultaAlRepositorioConLaCiudadRecortada() {
        when(repository.buscarClientesPorArtistaYCiudad(5L, " Tandil ")).thenReturn(List.of(3L, 4L));

        ClientesDeArtistaResponse respuesta = service.clientesPorArtistaYCiudad(5L, " Tandil ");

        assertEquals(5L, respuesta.idArtista());
        assertEquals("Tandil", respuesta.ciudad());
        assertEquals(List.of(3L, 4L), respuesta.idsUsuarios());
    }

    @Test
    void ciudadVaciaEsUnaSolicitudInvalida() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.clientesPorArtistaYCiudad(5L, "  "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).buscarClientesPorArtistaYCiudad(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
