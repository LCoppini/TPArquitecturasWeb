package org.example.factory;

import java.sql.Connection;


public interface ConnectionManager {

    // Devuelve la conexión abierta contra el motor correspondiente.
    Connection getConnection();

    /**
     * Cierra la conexión y libera los recursos del motor.
     * Es parte del contrato porque no todos los motores se cierran igual:
     * MySQL alcanza con cerrar la Connection, mientras que Derby embebido
     * exige además un shutdown explícito del engine.
     */

    // Preguntar el lunes ---> Misma consulta ubicada en MySQLConnectionManager
    void shutdown();
}
