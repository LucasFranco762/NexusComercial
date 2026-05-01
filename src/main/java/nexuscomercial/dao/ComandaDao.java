package nexuscomercial.dao;

import nexuscomercial.model.Comanda;
import nexuscomercial.model.ComandaItem;
import nexuscomercial.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComandaDao {
    public void save(Comanda cda) throws SQLException {
        String sql = "INSERT INTO comandas(numero,cliente,abertura,status,total,limite,desconto) VALUES(?,?,?,?,?,?,0)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cda.getNumero()); ps.setString(2, cda.getCliente()); ps.setString(3, cda.getAbertura());
            ps.setString(4, cda.getStatus()); ps.setDouble(5, 0); ps.setDouble(6, cda.getLimite()); ps.executeUpdate();
        }
    }

    public List<Comanda> findByFilter(String filter) throws SQLException {
        String sql = "SELECT * FROM comandas WHERE numero LIKE ? OR cliente LIKE ? ORDER BY id DESC";
        List<Comanda> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + filter + "%"); ps.setString(2, "%" + filter + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void addItem(int comandaId, int produtoId, int qtd, double unit, double subtotal, int userId, String lancamento) throws SQLException {
        String sql = "INSERT INTO itens_comanda(comanda_id,produto_id,quantidade,valor_unitario,subtotal,usuario_id,lancamento,cancelado) VALUES(?,?,?,?,?,?,?,0)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, comandaId); ps.setInt(2, produtoId); ps.setInt(3, qtd); ps.setDouble(4, unit);
            ps.setDouble(5, subtotal); ps.setInt(6, userId); ps.setString(7, lancamento); ps.executeUpdate();
        }
    }

    public List<ComandaItem> listItems(int comandaId) throws SQLException {
        String sql = """
            SELECT i.*, p.nome produto_nome, u.nome usuario_nome
            FROM itens_comanda i
            JOIN produtos p ON p.id = i.produto_id
            JOIN usuarios u ON u.id = i.usuario_id
            WHERE i.comanda_id = ? ORDER BY i.id DESC""";
        List<ComandaItem> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, comandaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComandaItem it = new ComandaItem();
                    it.setId(rs.getInt("id")); it.setComandaId(rs.getInt("comanda_id")); it.setProdutoId(rs.getInt("produto_id"));
                    it.setProdutoNome(rs.getString("produto_nome")); it.setQuantidade(rs.getInt("quantidade"));
                    it.setValorUnitario(rs.getDouble("valor_unitario")); it.setSubtotal(rs.getDouble("subtotal"));
                    it.setUsuarioNome(rs.getString("usuario_nome")); it.setLancamento(rs.getString("lancamento"));
                    it.setCancelado(rs.getInt("cancelado") == 1); list.add(it);
                }
            }
        }
        return list;
    }

    public void cancelItem(int itemId, int adminId, String when) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE itens_comanda SET cancelado=1,cancelado_por=?,cancelado_em=? WHERE id=?")) {
            ps.setInt(1, adminId); ps.setString(2, when); ps.setInt(3, itemId); ps.executeUpdate();
        }
    }

    public void updateComandaTotals(int comandaId) throws SQLException {
        String sql = "UPDATE comandas SET total = (SELECT COALESCE(SUM(subtotal),0) FROM itens_comanda WHERE comanda_id=? AND cancelado=0) WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, comandaId); ps.setInt(2, comandaId); ps.executeUpdate();
        }
    }

    public void updateStatus(int comandaId, String status) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE comandas SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, comandaId); ps.executeUpdate();
        }
    }

    public void updateDiscount(int comandaId, double discount) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE comandas SET desconto=? WHERE id=?")) {
            ps.setDouble(1, discount); ps.setInt(2, comandaId); ps.executeUpdate();
        }
    }

    public void closeComanda(int comandaId, String fechamento) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE comandas SET status='FECHADA', fechamento=? WHERE id=?")) {
            ps.setString(1, fechamento); ps.setInt(2, comandaId); ps.executeUpdate();
        }
    }

    public void savePayment(int comandaId, String forma, double bruto, double desconto, double totalFinal, String operador, String fechadoEm) throws SQLException {
        String sql = "INSERT INTO pagamentos(comanda_id,forma,total_bruto,desconto,total_final,operador,fechado_em) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, comandaId); ps.setString(2, forma); ps.setDouble(3, bruto); ps.setDouble(4, desconto);
            ps.setDouble(5, totalFinal); ps.setString(6, operador); ps.setString(7, fechadoEm); ps.executeUpdate();
        }
    }

    private Comanda map(ResultSet rs) throws SQLException {
        Comanda c = new Comanda();
        c.setId(rs.getInt("id")); c.setNumero(rs.getString("numero")); c.setCliente(rs.getString("cliente"));
        c.setAbertura(rs.getString("abertura")); c.setFechamento(rs.getString("fechamento")); c.setStatus(rs.getString("status"));
        c.setTotal(rs.getDouble("total")); c.setLimite(rs.getDouble("limite")); c.setDesconto(rs.getDouble("desconto"));
        return c;
    }
}
