package nexuscomercial.service;

import nexuscomercial.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
}
