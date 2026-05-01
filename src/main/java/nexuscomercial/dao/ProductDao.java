package nexuscomercial.dao;

import nexuscomercial.model.Product;
import nexuscomercial.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    public List<Product> findAll() throws SQLException {
        String sql = """
            SELECT p.*, c.nome as categoria_nome
            FROM produtos p LEFT JOIN categorias c ON c.id = p.categoria_id
            ORDER BY p.nome""";
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Product> findActiveInStock() throws SQLException {
        String sql = """
            SELECT p.*, c.nome as categoria_nome
            FROM produtos p LEFT JOIN categorias c ON c.id = p.categoria_id
            WHERE p.ativo = 1 AND p.estoque > 0 ORDER BY p.nome""";
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public void save(Product p) throws SQLException {
        String sql = "INSERT INTO produtos(codigo,nome,categoria_id,preco,estoque,estoque_minimo,ativo) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, p); ps.executeUpdate();
        }
    }

    public void update(Product p) throws SQLException {
        String sql = "UPDATE produtos SET codigo=?,nome=?,categoria_id=?,preco=?,estoque=?,estoque_minimo=?,ativo=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, p); ps.setInt(8, p.getId()); ps.executeUpdate();
        }
    }

    public void changeStock(int productId, int delta) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE produtos SET estoque = estoque + ? WHERE id = ?")) {
            ps.setInt(1, delta); ps.setInt(2, productId); ps.executeUpdate();
        }
    }

    private void fill(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getCodigo()); ps.setString(2, p.getNome()); ps.setInt(3, p.getCategoriaId());
        ps.setDouble(4, p.getPreco()); ps.setInt(5, p.getEstoque()); ps.setInt(6, p.getEstoqueMinimo()); ps.setInt(7, p.isAtivo() ? 1 : 0);
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNome(rs.getString("nome"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setCategoriaNome(rs.getString("categoria_nome"));
        p.setPreco(rs.getDouble("preco"));
        p.setEstoque(rs.getInt("estoque"));
        p.setEstoqueMinimo(rs.getInt("estoque_minimo"));
        p.setAtivo(rs.getInt("ativo") == 1);
        return p;
    }
}
