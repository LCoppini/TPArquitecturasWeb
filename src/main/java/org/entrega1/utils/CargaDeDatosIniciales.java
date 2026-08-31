package org.entrega1.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.entrega1.factory.DAOFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;


public class CargaDeDatosIniciales {

    //Constantes de Dao

      private final ClienteDAO clienteDAO;
      private final FacturaDAO facturaDAO;
      private final FacturaProductoDAO factura_produtoDAO;
      private final ProductoDAO productoDAO;

    public CargaDeDatosIniciales() {
        DAOFactory f =DAOFactory.getInstances();
        this.clienteDAO = f.createClienteDAO();
        this.facturaDAO = f.crearFacturaDAO();
        this.factura_productoDAO = f.crearFacturaproducto();
        this.productoDAO = f.crearProductoDAO();
    }
    // Preguntarrrr
    public void cargarClientes(String resourcePath){
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                    .parse(new FileReader("clientes.csv"))) {

                for (CSVRecord row : parser) {
                    String nombre = row.get("nombre");
                    String email = row.get("email");

                    clienteDAO.crearUsuarioDAO(new Cliente(null, nombre, email));
                }

            } catch (IOException e) {
                throw new RuntimeException("Error cargando clientes desde clientes.csv", e);
        }
    }

    private void cargarProductos(String resourcePath) {
        //Carga de productos.
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                .parse(new FileReader("productos.csv"))) {

            for (CSVRecord row : parser) {
                String nombre = row.get("nombre");
                Integer valor = Integer.parseInt(row.get("valor"));

                productosDAO.crearProductoDAO(new Productos(null, nombre, valor));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error cargando prodcutos", e);
        }
    }

    private void cargarFactura(String resourcePath) {
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                .parse(new FileReader("facturas.csv"))) {

            for (CSVRecord row : parser) {
                Integer idFactura = Integer.parseInt(row.get("idFactura"));
                Integer idCliente = Integer.parseInt(row.get("idCliente"));

                facturasDAO.crearFacturaDAO(new Facturas(idFactura, idCliente));
            }
            // podria ir un mensaje
        } catch (IOException e) {
            throw new RuntimeException("Error cargando prodcutos", e);
        }

    }

    private void cargarYRecalcularFacturasproductos(String resourcePath) {
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                .parse(new FileReader("facturas-productos.csv"))) {

            for (CSVRecord row : parser) {
                Integer idFactura = Integer.parseInt(row.get("idFactura"));
                Integer idProducto = Integer.parseInt(row.get("idProducto"));
                Integer cantidad = Integer.parseInt(row.get("cantidad"));

                facturasProductosDAO.crearFacturaPedidoDAO(new Facturasproductos(idFactura,idProducto, cantidad));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error cargando prodcutos", e);
        }

    }
    private InputStream mustGetResource(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) throw new IllegalArgumentException("Recurso no encontrado: " + path);
        return is;
    }

}
