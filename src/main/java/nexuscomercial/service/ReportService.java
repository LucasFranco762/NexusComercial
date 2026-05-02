package nexuscomercial.service;

import nexuscomercial.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    public String dailyReport(String date) {
        StringBuilder sb = new StringBuilder("Relatorio diario - ").append(date).append("\n\n");
        try (Connection c = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) qtd, COALESCE(SUM(total_final),0) total FROM pagamentos WHERE date(fechado_em)=date(?)")) {
                ps.setString(1, date);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sb.append("Comandas fechadas: ").append(rs.getInt("qtd")).append("\n");
                        sb.append("Total vendido: ").append(String.format("%.2f", rs.getDouble("total"))).append("\n");
                    }
                }
            }
            sb.append("Formas de pagamento:\n");
            try (PreparedStatement ps = c.prepareStatement("SELECT forma, COUNT(*) qtd FROM pagamentos WHERE date(fechado_em)=date(?) GROUP BY forma")) {
                ps.setString(1, date);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) sb.append("- ").append(rs.getString("forma")).append(": ").append(rs.getInt("qtd")).append("\n");
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return sb.toString();
    }

    public List<String> closedComandas(String filter) {
        List<String> list = new ArrayList<>();
        String f = "%" + (filter == null ? "" : filter) + "%";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT numero, cliente, fechamento, total, desconto FROM comandas WHERE status='FECHADA' AND (numero LIKE ? OR cliente LIKE ? OR fechamento LIKE ?) ORDER BY id DESC")) {
            ps.setString(1, f); ps.setString(2, f); ps.setString(3, f);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add("#" + rs.getString("numero") + " | " + rs.getString("cliente") + " | " + rs.getString("fechamento") + " | " +
                        String.format("%.2f", rs.getDouble("total") - rs.getDouble("desconto")));
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public List<String> closedComandaValues() {
        List<String> list = new ArrayList<>();
        String sql = """
            SELECT c.numero, c.cliente, p.total_final, p.fechado_em
            FROM pagamentos p
            JOIN comandas c ON c.id = p.comanda_id
            ORDER BY p.id DESC""";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add("#" + rs.getString("numero") + " | " + rs.getString("cliente") + " | " +
                    String.format("%.2f", rs.getDouble("total_final")) + " | " + rs.getString("fechado_em"));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public Map<String, String> caixaResumo() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("totalVendido", "R$ 0.00");
        m.put("comandasAbertas", "0");
        m.put("comandasFechadas", "0");
        m.put("comandaMaior", "-");
        m.put("comandaMenor", "-");
        m.put("lucroLiquido", "R$ 0.00");
        try (Connection c = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(total_final),0) total FROM pagamentos");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) m.put("totalVendido", "R$ " + String.format("%.2f", rs.getDouble("total")));
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) qtd FROM comandas WHERE status='ABERTA' OR status='BLOQUEADA'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) m.put("comandasAbertas", String.valueOf(rs.getInt("qtd")));
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) qtd FROM comandas WHERE status='FECHADA'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) m.put("comandasFechadas", String.valueOf(rs.getInt("qtd")));
            }
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT c.numero, c.cliente, p.total_final
                FROM pagamentos p JOIN comandas c ON c.id = p.comanda_id
                ORDER BY p.total_final DESC LIMIT 1""");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.put("comandaMaior",
                        rs.getString("numero") + "\n" +
                            rs.getString("cliente") + " - R$ " + String.format("%.2f", rs.getDouble("total_final")));
                }
            }
            try (PreparedStatement ps = c.prepareStatement("""
                SELECT c.numero, c.cliente, p.total_final
                FROM pagamentos p JOIN comandas c ON c.id = p.comanda_id
                ORDER BY p.total_final ASC LIMIT 1""");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.put("comandaMenor",
                        rs.getString("numero") + "\n" +
                            rs.getString("cliente") + " - R$ " + String.format("%.2f", rs.getDouble("total_final")));
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return m;
    }

    public List<String> resumoPorFormaPagamento() {
        List<String> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT forma, COUNT(*) qtd, COALESCE(SUM(total_final),0) total FROM pagamentos GROUP BY forma ORDER BY total DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("forma") + ": " + rs.getInt("qtd") + "  |  R$ " + String.format("%.2f", rs.getDouble("total")));
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public List<String> resumoItensVendidos() {
        List<String> list = new ArrayList<>();
        String sql = """
            SELECT p.nome, SUM(i.quantidade) qtd
            FROM itens_comanda i
            JOIN produtos p ON p.id = i.produto_id
            JOIN comandas c ON c.id = i.comanda_id
            WHERE i.cancelado = 0 AND c.status='FECHADA'
            GROUP BY p.nome ORDER BY qtd DESC""";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("nome") + ": " + String.format("%02d", rs.getInt("qtd")));
        } catch (Exception e) { throw new RuntimeException(e); }
        return list;
    }

    public String itemMaisVendido() {
        return extremoItem(true);
    }

    public String itemMenosVendido() {
        return extremoItem(false);
    }

    private String extremoItem(boolean maior) {
        String ord = maior ? "DESC" : "ASC";
        String sql = """
            SELECT p.nome, SUM(i.quantidade) qtd
            FROM itens_comanda i
            JOIN produtos p ON p.id = i.produto_id
            JOIN comandas c ON c.id = i.comanda_id
            WHERE i.cancelado = 0 AND c.status='FECHADA'
            GROUP BY p.nome
            ORDER BY qtd
            """ + ord + " LIMIT 1";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("nome") + ": " + rs.getInt("qtd");
        } catch (Exception e) { throw new RuntimeException(e); }
        return "-";
    }
}
