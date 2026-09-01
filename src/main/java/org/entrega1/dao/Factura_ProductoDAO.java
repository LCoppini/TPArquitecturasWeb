package org.entrega1.dao;

import org.entrega1.entity.Factura_Producto;

import java.util.List;

public interface Factura_ProductoDAO {
    Factura_Producto findById(Long id);
    List<Factura_Producto> findByFactura(Long idFactura);

    void create(Factura_Producto fp);
    void update(Factura_Producto fp);
    void delete(Long idFactura);
    void deleteAll();


}
