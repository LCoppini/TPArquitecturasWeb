package org.entrega1;

import org.entrega1.dao.ClienteDAO;
import org.entrega1.dao.FacturaDAO;
import org.entrega1.dao.Factura_ProductoDAO;
import org.entrega1.dao.ProductoDAO;
import org.entrega1.factory.DAOFactory;
import org.entrega1.factory.DBType;
import org.entrega1.repository.MySQLSchemaInitializer;
import org.entrega1.utils.CargaDeDatosIniciales;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static final DBType MOTOR = DBType.MYSQL;

    public static void main(String[] args) {

        // no correr ya creados

        MySQLSchemaInitializer.crearEsquema();
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

        /**Cliente uno = ClienteDAO.findById(1L);
        System.out.println("findById: " + uno);

        uno.setEdad(37);
        usuarioDAO.update(uno);*/


        /**System.out.println("Todos los usuarios: " + clienteDAO.findAll());

        System.out.println("Todos los productos: " + productoDAO.findAll());

        System.out.println("Todos los pedidos: " + facturaDAO.findAll());
        */
    }
}