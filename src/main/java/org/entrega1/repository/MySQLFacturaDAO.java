package org.entrega1.repository;

import org.entrega1.dao.FacturaDAO;
import org.entrega1.entity.Factura;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLFacturaDAO implements FacturaDAO {

    private final Connection connection;

    public MySQLFacturaDAO(Connection connection) {
        this.connection = connection;
    }

    private Factura map(ResultSet rs) throws SQLException {
        return new Factura(
                rs.getLong("idFactura"),
                rs.getLong("idCliente")
        );
    }

    @Override
    public Factura findById(Long idFactura) {
        String sql = "SELECT * FROM Factura WHERE idFactura = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando factura por id", e);
        }
    }

    @Override
    public List<Factura> findAll() {
        String sql = "SELECT * FROM Factura";
        List<Factura> facturas = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                facturas.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listando facturas", e);
        }
        return facturas;
    }

    @Override
    public List<Factura> findByCliente(Long idCliente) {
        String sql = "SELECT * FROM Factura WHERE idCliente = ?";
        List<Factura> facturas = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    facturas.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando facturas por cliente", e);
        }
        return facturas;
    }

    @Override
    public void create(Factura f) {
        String sql = "INSERT INTO Factura (idCliente) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, f.getIdCliente());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    f.setIdFactura(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creando factura", e);
        }
    }

    @Override
    public void update(Factura f) {
        String sql = "UPDATE Factura SET idCliente = ? WHERE idFactura = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, f.getIdCliente());
            ps.setLong(2, f.getIdFactura());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando factura", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM Factura WHERE idFactura = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando factura", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM Factura";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error borrando todas las facturas", e);
        }
    }
}
