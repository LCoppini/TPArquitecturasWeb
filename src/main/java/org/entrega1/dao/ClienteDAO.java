package org.entrega1.dao;

import org.entrega1.entity.Cliente;

import java.util.List;

public interface ClienteDAO {
    Cliente findById(Long id);
    List<Cliente> findAll();

    void create(Cliente c);
    void update(Cliente c);
    void delete(Long id);
    void deleteAll();

    List<Cliente>  findAllOrderByFacturacion();

}

