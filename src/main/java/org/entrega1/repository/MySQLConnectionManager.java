package org.entrega1.repository;

import org.entrega1.factory.ConnectionManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySQLConnectionManager implements ConnectionManager {

    private static volatile MySQLConnectionManager instance;
    private Connection connection;

    // Config de conexion
    private static final String URL = "jdbc:mysql://localhost:3307/mysql_dao_DB?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // ----- Constructor privado
    private MySQLConnectionManager() {
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database successfully");
        }
        catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de MySQL.");
            e.printStackTrace();
        } catch (
                SQLException e) {
            System.err.println("Error al conectar con la base de datos.");
            e.printStackTrace();
        }
    }

    // Delegacion del new
    public static MySQLConnectionManager getInstance() {
        if(instance == null){
            synchronized (MySQLConnectionManager.class) {
                if(instance == null){
                    instance = new MySQLConnectionManager();
                }
            }
        }
        return instance;
    }

    // - Retornar la cone
    @Override
    public Connection getConnection() {
        return connection;
    }

    // Preguntar el lunes ----> ¿Es necesario este metodo aunque no se use derby?
    @Override
    public void shutdown(){
        try{
            this.getConnection().close();
        }catch(SQLException e){
            e.printStackTrace();
        }
    }


}


