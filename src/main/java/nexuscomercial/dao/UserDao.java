package nexuscomercial.dao;

import nexuscomercial.model.User;
import nexuscomercial.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public User findByCredentials(String usuario, String senha) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE usuario = ? AND senha = ? AND status = 'ATIVO'";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, senha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM usuarios ORDER BY nome"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void save(User u) throws SQLException {
        String sql = "INSERT INTO usuarios(nome,usuario,senha,perfil,status) VALUES(?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNome()); ps.setString(2, u.getUsuario()); ps.setString(3, u.getSenha());
            ps.setString(4, u.getPerfil()); ps.setString(5, u.getStatus()); ps.executeUpdate();
        }
    }

    public void update(User u) throws SQLException {
        String sql = "UPDATE usuarios SET nome=?, usuario=?, senha=?, perfil=?, status=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getNome()); ps.setString(2, u.getUsuario()); ps.setString(3, u.getSenha());
            ps.setString(4, u.getPerfil()); ps.setString(5, u.getStatus()); ps.setInt(6, u.getId()); ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("nome"), rs.getString("usuario"),
            rs.getString("senha"), rs.getString("perfil"), rs.getString("status"));
    }
}

