package nexuscomercial.util;

import nexuscomercial.model.Comanda;
import nexuscomercial.model.ComandaItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ReceiptUtil {
    private ReceiptUtil() {}

    public static Path generate(Comanda c, List<ComandaItem> items, String formaPagamento, String operador, Map<String, String> cfg) {
        StringBuilder txt = new StringBuilder();
        txt.append(cfg.getOrDefault("nome_estabelecimento", "NexusComercial")).append("\n");
        txt.append("Comanda #").append(c.getNumero()).append(" - ").append(c.getCliente()).append("\n");
        txt.append("Abertura: ").append(c.getAbertura()).append("\n");
        txt.append("Fechamento: ").append(DateUtil.now()).append("\n\n");
        txt.append("Itens:\n");
        for (ComandaItem i : items) {
            if (!i.isCancelado()) txt.append("- ").append(i.getProdutoNome()).append(" x").append(i.getQuantidade()).append(" = ").append(String.format("%.2f", i.getSubtotal())).append("\n");
        }
        txt.append("\nTotal bruto: ").append(String.format("%.2f", c.getTotal())).append("\n");
        txt.append("Desconto: ").append(String.format("%.2f", c.getDesconto())).append("\n");
        txt.append("Total final: ").append(String.format("%.2f", c.getTotal() - c.getDesconto())).append("\n");
        txt.append("Pagamento: ").append(formaPagamento).append("\n");
        txt.append("Operador: ").append(operador).append("\n\n");
        txt.append(cfg.getOrDefault("mensagem_recibo", "Obrigado pela preferencia!")).append("\n");
        try {
            Path output = Path.of("recibo_comanda_" + c.getNumero() + ".txt");
            Files.writeString(output, txt.toString());
            return output;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
