package org.entrega1;

import org.entrega1.dao.ClienteDAO;
import org.entrega1.dao.FacturaDAO;
import org.entrega1.dao.Factura_ProductoDAO;
import org.entrega1.dao.ProductoDAO;
import org.entrega1.dto.ClienteMayorFacturacion;
import org.entrega1.dto.ProductoMasRecaudoDTO;
import org.entrega1.factory.DAOFactory;
import org.entrega1.factory.DBType;
import org.entrega1.repository.MySQLClienteDAO;
import org.entrega1.repository.MySQLConnectionManager;
import org.entrega1.repository.MySQLProductoDAO;
import org.entrega1.repository.MySQLSchemaInitializer;
import org.entrega1.utils.CargaDeDatosIniciales;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final DBType MOTOR = DBType.MYSQL;

    public static void main(String[] args) {

        // no correr ya creados

        /*MySQLSchemaInitializer.crearEsquema();
        final var carga = new CargaDeDatosIniciales();

        carga.cargarClientes("src/main/resources/data/clientes.csv");
        carga.cargarProductos("src/main/resources/data/productos.csv");
        carga.cargarFactura("src/main/resources/data/facturas.csv");
        carga.cargarFacturasproductos("src/main/resources/data/facturas-productos.csv");

        System.out.println("Carga Inicial");
        
        DAOFactory f = DAOFactory.getInstance(); // resuelve segun db.type, ya fijado arriba

        ClienteDAO clienteDAO = f.createClienteDAO();
        ProductoDAO productoDAO=f.crearProductoDAO();
        FacturaDAO facturaDAO = f.crearFacturaDAO();
        Factura_ProductoDAO facturaProductoDAO = f.crearFacturaproducto();

         */

        Connection conn = MySQLConnectionManager.getInstance().getConnection();
        MySQLProductoDAO productoDAO = new MySQLProductoDAO(conn);
        ProductoMasRecaudoDTO productoMasRecaudado = productoDAO.prodMasRecaudacion();

        if (productoMasRecaudado != null) {
            System.out.println("Producto MasRecaudado");
            System.out.println("Producto: " + productoMasRecaudado.getNombre());
            System.out.println("Total: " + productoMasRecaudado.getTotalRecaudado());
        }

        Connection connn = MySQLConnectionManager.getInstance().getConnection();
        MySQLClienteDAO clienteDAO = new MySQLClienteDAO(connn);
        List<ClienteMayorFacturacion> clientesOrdenados =  clienteDAO.findAllOrderByFacturacion();

        System.out.println("Clientes Ordenados" + clientesOrdenados);


    }
}