package com.ticketya.entrada.service;

import com.ticketya.entrada.dto.request.RegistroPagoRequest;
import com.ticketya.entrada.dto.response.RegistroPagoResponse;
import com.ticketya.entrada.entity.Entrada;
import com.ticketya.entrada.entity.Pago;
import com.ticketya.entrada.entity.RegistroPago;
import com.ticketya.entrada.entity.RegistroPagoId;
import com.ticketya.entrada.repository.EntradaRepository;
import com.ticketya.entrada.repository.PagoRepository;
import com.ticketya.entrada.repository.RegistroPagoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RegistroPagoService {

    private final RegistroPagoRepository registroPagoRepository;
    private final EntradaRepository entradaRepository;
    private final PagoRepository pagoRepository;

    public RegistroPagoService(
            RegistroPagoRepository registroPagoRepository,
            EntradaRepository entradaRepository,
            PagoRepository pagoRepository
    ) {
        this.registroPagoRepository = registroPagoRepository;
        this.entradaRepository = entradaRepository;
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public RegistroPagoResponse crear(RegistroPagoRequest request) {
        throw new IllegalStateException("Los registros de pago se generan al confirmar una compra");
    }

    public List<RegistroPagoResponse> listar(Long idUsuario) {
        return registroPagoRepository.findByUsuario(idUsuario).stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    public RegistroPagoResponse buscarPorId(Long idEntrada, Long idPago, Long idUsuario) {
        RegistroPagoId id = crearId(idEntrada, idPago);
        return registroPagoRepository.findByIdAndUsuario(idEntrada, idPago, idUsuario)
                .map(this::convertirRespuesta)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el registro de pago para entrada " + id.getIdEntrada()
                                + " y pago " + id.getIdPago()));
    }

    @Transactional
    public RegistroPagoResponse actualizar(Long idEntrada, Long idPago, RegistroPagoRequest datos) {
        throw new IllegalStateException("Los registros de pago son inmutables");
    }

    @Transactional
    public void eliminar(Long idEntrada, Long idPago) {
        throw new IllegalStateException("Los registros de pago no se pueden eliminar");
    }

    private RegistroPagoId crearId(Long idEntrada, Long idPago) {
        RegistroPagoId id = new RegistroPagoId();
        id.setIdEntrada(idEntrada);
        id.setIdPago(idPago);
        return id;
    }

    private RegistroPago buscarEntidadPorId(RegistroPagoId id) {
        return registroPagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el registro de pago para entrada " + id.getIdEntrada()
                                + " y pago " + id.getIdPago()));
    }

    private void aplicarDatos(RegistroPago registroPago, RegistroPagoRequest request) {
        registroPago.setFechaPago(request.getFechaPago());
        registroPago.setDescripcion(request.getDescripcion());
        registroPago.setMontoTotal(request.getMontoTotal());
    }

    private RegistroPagoResponse convertirRespuesta(RegistroPago registroPago) {
        return new RegistroPagoResponse(
                registroPago.getId().getIdEntrada(),
                registroPago.getId().getIdPago(),
                registroPago.getFechaPago(),
                registroPago.getDescripcion(),
                registroPago.getMontoTotal()
        );
    }
}
