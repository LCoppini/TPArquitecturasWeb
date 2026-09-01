package org.entrega1.dao;

import java.util.List;

public interface FacturaProductoDAO {
    FacturaProducto findById(Long id);
    List<FacturaProducto> findByFactura(Long idFactura);

    void create(FacturaProducto fp);
    void update(FacturaProducto fp);
    void delete(Long idFactura);
    void deleteAll();


}
