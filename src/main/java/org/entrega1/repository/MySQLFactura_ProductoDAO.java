package org.entrega1.repository;

import org.entrega1.dao.Factura_ProductoDAO;
import org.entrega1.entity.Factura_Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLFactura_ProductoDAO implements Factura_ProductoDAO {

    private final Connection connection;

    public MySQLFactura_ProductoDAO(Connection connection) {
        this.connection = connection;
    }

    private Factura_Producto map(ResultSet rs) throws SQLException {
        return new Factura_Producto(
                rs.getLong("id"),
                rs.getLong("idFactura"),
                rs.getLong("idProducto"),
                rs.getInt("cantidad")
        );
    }

    @Override
    public Factura_Producto findById(Long id) {
        String sql = "SELECT * FROM Factura_Producto WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando factura_producto por id", e);
        }
    }

    @Override
    public List<Factura_Producto> findByFactura(Long idFactura) {
        String sql = "SELECT * FROM Factura_Producto WHERE idFactura = ?";
        List<Factura_Producto> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando items por factura", e);
        }
        return items;
    }

    @Override
    public List<Factura_Producto> findByProducto(Long idProducto) {
        String sql = "SELECT * FROM Factura_Producto WHERE idProducto = ?";
        List<Factura_Producto> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando items por producto", e);
        }
        return items;
    }

    @Override
    public void create(Factura_Producto fp) {
        String sql = "INSERT INTO Factura_Producto (idFactura, idProducto, cantidad) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, fp.getIdFactura());
            ps.setLong(2, fp.getIdProducto());
            ps.setInt(3, fp.getCantidad());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    fp.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando factura_producto", e);
        }
    }

    @Override
    public void update(Factura_Producto fp) {
        String sql = "UPDATE Factura_Producto SET idFactura = ?, idProducto = ?, cantidad = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, fp.getIdFactura());
            ps.setLong(2, fp.getIdProducto());
            ps.setInt(3, fp.getCantidad());
            ps.setLong(4, fp.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando factura_producto", e);
        }
    }

    @Override
    public void delete(Long idFactura) {
        String sql = "DELETE FROM Factura_Producto WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idFactura);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando factura_producto", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM Factura_Producto";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando todos los factura_producto", e);
        }
    }
}
