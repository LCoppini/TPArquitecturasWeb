package org.entrega1.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class MySQLSchemaInitializer {

    private MySQLSchemaInitializer() {}

    public static void crearEsquema() {
        Connection conn = MySQLConnectionManager.getInstance().getConnection();

        String cliente = """
            CREATE TABLE IF NOT EXISTS Cliente (
                idCliente INT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(500) NOT NULL,
                email VARCHAR(150)
            )""";

        String producto = """
            CREATE TABLE IF NOT EXISTS Producto (
                idProducto INT AUTO_INCREMENT PRIMARY KEY,
                nombre VARCHAR(45) NOT NULL,
                valor FLOAT NOT NULL
            )""";

        String factura = """
            CREATE TABLE IF NOT EXISTS Factura (
                idFactura INT AUTO_INCREMENT PRIMARY KEY,
                idCliente INT NOT NULL,
                CONSTRAINT fk_factura_cliente FOREIGN KEY (idCliente)
                    REFERENCES Cliente(idCliente)
            )""";

        String facturaProducto = """
            CREATE TABLE IF NOT EXISTS Factura_Producto (
                id INT AUTO_INCREMENT PRIMARY KEY,
                idFactura INT NOT NULL,
                idProducto INT NOT NULL,
                cantidad INT NOT NULL,
                CONSTRAINT fk_fp_factura FOREIGN KEY (idFactura)
                    REFERENCES Factura(idFactura),
                CONSTRAINT fk_fp_producto FOREIGN KEY (idProducto)
                    REFERENCES Producto(idProducto)
            )""";

        try (Statement st = conn.createStatement()) {
            st.execute(cliente);
            st.execute(producto);
            st.execute(factura);
            st.execute(facturaProducto);
            System.out.println("Esquema creado (o ya existía).");
        } catch (SQLException e) {
            throw new RuntimeException("Error creando el esquema de la base de datos", e);
        }
    }
}
