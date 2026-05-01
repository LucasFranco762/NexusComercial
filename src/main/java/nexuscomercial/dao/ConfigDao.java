package nexuscomercial.dao;

import nexuscomercial.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConfigDao {
    public Map<String, String> getAll() throws SQLException {
        Map<String, String> map = new HashMap<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT chave, valor FROM configuracoes"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("chave"), rs.getString("valor"));
        }
        return map;
    }

    public void save(String key, String value) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO configuracoes(chave,valor) VALUES(?,?) ON CONFLICT(chave) DO UPDATE SET valor=excluded.valor")) {
            ps.setString(1, key); ps.setString(2, value); ps.executeUpdate();
        }
    }
}
