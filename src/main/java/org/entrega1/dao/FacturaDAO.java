package org.entrega1.dao;

import org.entrega1.entity.Factura;

import java.util.List;

public interface FacturaDAO {

    Factura findById(Long idFactura);
    List<Factura> findAll();
    List<Factura> findByCliente(Long idCliente);

    void create(Factura f);
    void update(Factura f);
    void delete(Long id);
    void deleteAll();

}
