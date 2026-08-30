package org.example.repository;

import org.example.factory.DAOFactory;

import java.sql.Connection;

public class MySQLDAOFactory extends DAOFactory {



    @Override
    protected Connection getConnection() {
        return MySQLConnectionManager.getInstance().getConnection();
    }

    /** Cierre especifico de MySQL: delega en su propio gestor de conexiones. */
    @Override
    protected void doShutdown() {
        MySQLConnectionManager.getInstance().shutdown();
    }

    // Retornan las implementaciones concretas MYSQL de los DAOS

    @Override
    public UsuarioDAO crearUsuarioDAO() {
        return new MySQLUsuarioDAO(getConnection());
    }

    @Override
    public ProductoDAO crearProductoDAO() {
        return new MySQLProductoDAO(getConnection());
    }

    @Override
    public PedidoDAO crearPedidoDAO() {
        return new MySQLPedidoDAO(getConnection());
    }

    @Override
    public DetallePedidoDAO crearDetallePedidoDAO() {
        return new MySQLDetallePedidoDAO(getConnection());
    }



}
