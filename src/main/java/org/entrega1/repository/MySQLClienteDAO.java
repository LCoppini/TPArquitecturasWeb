package org.entrega1.repository;

import org.entrega1.dao.ClienteDAO;
import org.entrega1.entity.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLClienteDAO implements ClienteDAO {

    private final Connection connection;

    public MySQLClienteDAO(Connection connection) {
        this.connection = connection;
    }

    private Cliente map(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getLong("idCliente"),
                rs.getString("nombre"),
                rs.getString("email")
        );
    }

    @Override
    public Cliente findById(Long idCliente) {
        String sql = "SELECT * FROM Cliente WHERE idCliente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando cliente por id", e);
        }
    }

    @Override
    public List<Cliente> findAll() {
        String sql = "SELECT * FROM Cliente";
        List<Cliente> clientes = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                clientes.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando clientes", e);
        }
        return clientes;
    }

    @Override
    public void create(Cliente c) {
        String sql = "INSERT INTO Cliente (nombre, email) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setIdCliente(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando cliente", e);
        }
    }

    @Override
    public void update(Cliente c) {
        String sql = "UPDATE Cliente SET nombre = ?, email = ? WHERE idCliente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getEmail());
            ps.setLong(3, c.getIdCliente());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando cliente", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM Cliente WHERE idCliente = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando cliente", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM Cliente";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando todos los clientes", e);
        }
    }

    // Ejercicio Integrador punto 4: clientes ordenados por lo que se les facturó (desc).
    @Override
    public List<Cliente> findAllOrderByFacturacion() {
        String sql = """
            SELECT c.idCliente, c.nombre, c.email,
                   COALESCE(SUM(fp.cantidad * p.valor), 0) AS total_facturado
            FROM Cliente c
            LEFT JOIN Factura f ON f.idCliente = c.idCliente
            LEFT JOIN Factura_Producto fp ON fp.idFactura = f.idFactura
            LEFT JOIN Producto p ON p.idProducto = fp.idProducto
            GROUP BY c.idCliente, c.nombre, c.email
            ORDER BY total_facturado DESC
            """;
        List<Cliente> clientes = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                clientes.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando clientes ordenados por facturación", e);
        }
        return clientes;
    }
}
