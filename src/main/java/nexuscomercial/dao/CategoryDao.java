package nexuscomercial.dao;

import nexuscomercial.model.Category;
import nexuscomercial.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {
    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM categorias ORDER BY nome"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Category(rs.getInt("id"), rs.getString("nome")));
        }
        return list;
    }

    public void save(String nome) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO categorias(nome) VALUES(?)")) {
            ps.setString(1, nome); ps.executeUpdate();
        }
    }

    public void update(int id, String nome) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE categorias SET nome=? WHERE id=?")) {
            ps.setString(1, nome); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM categorias WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }
}
