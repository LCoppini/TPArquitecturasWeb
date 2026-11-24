package com.ticketya.preferencias.service.impl;

import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.dto.response.ArtistaPreferidoResponse;
import com.ticketya.preferencias.dto.response.ClientesDeArtistaResponse;
import com.ticketya.preferencias.dto.response.CompraRegistradaResponse;
import com.ticketya.preferencias.dto.response.PreferenciaClienteResponse;
import com.ticketya.preferencias.entity.PreferenciaCliente;
import com.ticketya.preferencias.repository.PreferenciaClienteRepository;
import com.ticketya.preferencias.service.PreferenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PreferenciaServiceImpl implements PreferenciaService {

    private final PreferenciaClienteRepository repository;

    public PreferenciaServiceImpl(PreferenciaClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompraRegistradaResponse registrarCompra(CompraRegistradaRequest compra) {
        boolean yaProcesada = repository.existeCompra(compra.idUsuario(), compra.idEntrada());
        // Se llama siempre: la condición de idempotencia vive en el updateOne atómico
        // del repositorio, no en esta consulta previa (que solo informa la respuesta).
        repository.registrarCompra(compra);
        return new CompraRegistradaResponse(compra.idEntrada(), yaProcesada);
    }

    @Override
    public ClientesDeArtistaResponse clientesPorArtistaYCiudad(Long idArtista, String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ciudad es obligatoria");
        }
        return new ClientesDeArtistaResponse(idArtista, ciudad.trim(),
                repository.buscarClientesPorArtistaYCiudad(idArtista, ciudad));
    }

    @Override
    public PreferenciaClienteResponse obtener(Long idUsuario) {
        PreferenciaCliente preferencia = repository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe preferencia para el usuario " + idUsuario));

        List<ArtistaPreferidoResponse> artistas = preferencia.getArtistas().stream()
                .map(a -> new ArtistaPreferidoResponse(a.getIdArtista(), a.getNombre(), a.getCantidad()))
                .toList();
        return new PreferenciaClienteResponse(preferencia.getIdUsuario(), preferencia.getCiudad(),
                preferencia.getTiposEvento(), artistas);
    }
}
