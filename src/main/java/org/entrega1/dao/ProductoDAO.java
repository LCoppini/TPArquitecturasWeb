package org.entrega1.dao;

import org.entrega1.entity.Producto;

import java.util.List;

public interface ProductoDAO {
    Producto findById(Long idProducto);
    List<Producto> findAll();

    void create(Producto p);
    void update(Producto p);
    void delete(Long idProducto);
    void deleteAll();

    Producto prodMasRecaudacion();
}
