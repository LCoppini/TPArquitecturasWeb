package org.entrega1.repository;

import org.entrega1.dao.ClienteDAO;
import org.entrega1.dao.FacturaDAO;
import org.entrega1.dao.Factura_ProductoDAO;
import org.entrega1.dao.ProductoDAO;
import org.entrega1.factory.DAOFactory;

import java.sql.Connection;

public class MySQLDAOFactory extends DAOFactory {

    @Override
    protected Connection getConnection() {
        return MySQLConnectionManager.getInstance().getConnection();
    }

    /** Cierre específico de MySQL: delega en su propio gestor de conexiones. */
    @Override
    protected void doShutdown() {
        MySQLConnectionManager.getInstance().shutdown();
    }

    // Retornan las implementaciones concretas MYSQL de los DAOS

    @Override
    public ClienteDAO createClienteDAO() {
        return new MySQLClienteDAO(getConnection());
    }

    @Override
    public ProductoDAO crearProductoDAO() {
        return new MySQLProductoDAO(getConnection());
    }

    @Override
    public FacturaDAO crearFacturaDAO() {
        return new MySQLFacturaDAO(getConnection());
    }

    @Override
    public Factura_ProductoDAO crearFacturaproducto() {
        return new MySQLFactura_ProductoDAO(getConnection());
    }
}
