package com.ticketya.preferencias.repository;

import com.ticketya.preferencias.dto.request.CompraRegistradaRequest;
import com.ticketya.preferencias.entity.PreferenciaCliente;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PreferenciaClienteRepository
        extends MongoRepository<PreferenciaCliente, Long>, PreferenciaClienteRepositoryCustom {
}
