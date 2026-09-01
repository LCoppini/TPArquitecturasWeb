package org.entrega1.dao;

import java.util.List;

public interface FacturaDAO {

    Factura findById(Long id);
    List<Factura> findAll();
    List<Factura> findByCliente(Long clienteId);

    void create(Factura f);
    void update(Factura f);
    void delete(Long id);
    void deleteAll();

}
