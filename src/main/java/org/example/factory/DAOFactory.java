package org.example.factory;

import org.example.repository.MySQLDAOFactory;

import java.sql.Connection;

public abstract class DAOFactory {

    private static volatile DAOFactory instance;

    public static DAOFactory getInstance(DBType dbType) {
        if (instance == null) {
            synchronized (DBType.class) {
                if (instance == null) {
                    switch (dbType) {
                        case MYSQL:
                            instance = new MySQLDAOFactory();
                            break;
//                        case DERBY:
//                            instance = new DerbyDAOFactory();
//                            break;

                        default:
                            throw new IllegalArgumentException("DBType no soportado: " + dbType);
                    }
                }
            }
        }
        return instance;
    }

    // Posibilidad de llamada sin indicar la dbtype, por defecto es myqsl
    public static DAOFactory getInstance() {
        String v = System.getProperty("db.type", "MYSQL");
        DBType type = DBType.valueOf(v.toUpperCase());
        return getInstance(type);
    }

    // ------ Factory Methods
    public abstract UsuarioDAO crearUsuarioDAO();
    public abstract ProductoDAO crearProductoDAO();
    public abstract PedidoDAO crearPedidoDAO();
    public abstract DetallePedidoDAO crearDetallePedidoDAO();

    protected abstract Connection getConnection();

    public final void shutdown() {
        doShutdown();
        synchronized (DAOFactory.class) {
            instance = null;
        }
    }

    /** Cada fabrica concreta cierra SU gestor de conexiones. */
    protected abstract void doShutdown();
}