package org.entrega1.dao;

import java.util.List;

public interface ProductoDAO {
    Producto findById(Long id);
    List<Producto> findAll();

    void create(Producto p);
    void update(Producto p);
    void delete(Long id);
    void deleteAll();

    Producto prodMasRecaudacion();
}
