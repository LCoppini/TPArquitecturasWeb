package org.entrega1.repository;

import org.entrega1.dao.ProductoDAO;
import org.entrega1.entity.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLProductoDAO implements ProductoDAO {

    private final Connection connection;

    public MySQLProductoDAO(Connection connection) {
        this.connection = connection;
    }

    private Producto map(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getLong("idProducto"),
                rs.getString("nombre"),
                rs.getFloat("valor")
        );
    }

    @Override
    public Producto findById(Long idProducto) {
        String sql = "SELECT * FROM Producto WHERE idProducto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando producto por id", e);
        }
    }

    @Override
    public List<Producto> findAll() {
        String sql = "SELECT * FROM Producto";
        List<Producto> productos = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                productos.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando productos", e);
        }
        return productos;
    }

    @Override
    public void create(Producto p) {
        String sql = "INSERT INTO Producto (nombre, valor) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setFloat(2, p.getValor());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setIdProducto(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando producto", e);
        }
    }

    @Override
    public void update(Producto p) {
        String sql = "UPDATE Producto SET nombre = ?, valor = ? WHERE idProducto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setFloat(2, p.getValor());
            ps.setLong(3, p.getIdProducto());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando producto", e);
        }
    }

    @Override
    public void delete(Long idProducto) {
        String sql = "DELETE FROM Producto WHERE idProducto = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idProducto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando producto", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM Producto";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando todos los productos", e);
        }
    }

    // Ejercicio Integrador punto 3: producto que más recaudó (cantidad vendida * valor).
    @Override
    public Producto prodMasRecaudacion() {
        String sql = """
            SELECT p.idProducto, p.nombre, p.valor,
                   SUM(fp.cantidad * p.valor) AS recaudacion
            FROM Producto p
            JOIN Factura_Producto fp ON fp.idProducto = p.idProducto
            GROUP BY p.idProducto, p.nombre, p.valor
            ORDER BY recaudacion DESC
            LIMIT 1
            """;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException("Error calculando el producto con mayor recaudación", e);
        }
    }
}
