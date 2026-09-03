package org.entrega1.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.entrega1.dao.ClienteDAO;
import org.entrega1.dao.FacturaDAO;
import org.entrega1.dao.Factura_ProductoDAO;
import org.entrega1.dao.ProductoDAO;
import org.entrega1.entity.Cliente;
import org.entrega1.entity.Factura;
import org.entrega1.entity.Factura_Producto;
import org.entrega1.entity.Producto;
import org.entrega1.factory.DAOFactory;


import java.io.FileReader;
import java.io.IOException;


public class CargaDeDatosIniciales {


      private final ClienteDAO clienteDAO;
      private final FacturaDAO facturaDAO;
      private final Factura_ProductoDAO factura_productoDAO;
      private final ProductoDAO productoDAO;

    public CargaDeDatosIniciales() {
        DAOFactory f =DAOFactory.getInstance();
        this.clienteDAO = f.createClienteDAO();
        this.facturaDAO = f.crearFacturaDAO();
        this.factura_productoDAO = f.crearFacturaproducto();
        this.productoDAO = f.crearProductoDAO();
    }
    // Preguntarrrr
    public void cargarClientes(String resourcePath){
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                    .parse(new FileReader(resourcePath))) {

                for (CSVRecord row : parser) {
                    String nombre = row.get("nombre");
                    String email = row.get("email");

                    clienteDAO.create(new Cliente(null, nombre, email));
                }

            } catch (IOException e) {
                throw new RuntimeException("Error cargando clientes desde clientes.csv", e);
        }
    }

    public void cargarProductos(String resourcePath) {
        //Carga de productos.
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                .parse(new FileReader(resourcePath))) {

            for (CSVRecord row : parser) {
                String nombre = row.get("nombre");
                float valor = Integer.parseInt(row.get("valor"));

                productoDAO.create(new Producto(null, nombre, valor));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error cargando prodcutos", e);
        }
    }

    public void cargarFactura(String resourcePath) {
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                .parse(new FileReader(resourcePath))) {

            for (CSVRecord row : parser) {
                Long idFactura = (long) Integer.parseInt(row.get("idFactura"));
                Long idCliente = (long) Integer.parseInt(row.get("idCliente"));

                facturaDAO.create(new Factura(idFactura, idCliente));
            }
            // podria ir un mensaje
        } catch (IOException e) {
            throw new RuntimeException("Error cargando prodcutos", e);
        }

    }

    public void cargarFacturasproductos(String resourcePath) {
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader()
                .parse(new FileReader(resourcePath))) {

            for (CSVRecord row : parser) {
                Long idFactura = (long) Integer.parseInt(row.get("idFactura"));
                Long idProducto = (long) Integer.parseInt(row.get("idProducto"));
                Integer cantidad = Integer.parseInt(row.get("cantidad"));

                factura_productoDAO.create(new Factura_Producto(null, idFactura,idProducto, cantidad));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error cargando prodcutos", e);
        }

    }


}
