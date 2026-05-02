package nexuscomercial.service;

import nexuscomercial.dao.ComandaDao;
import nexuscomercial.dao.ConfigDao;
import nexuscomercial.dao.ProductDao;
import nexuscomercial.model.Comanda;
import nexuscomercial.model.ComandaItem;
import nexuscomercial.model.Product;
import nexuscomercial.util.DateUtil;
import nexuscomercial.util.SessionContext;

import java.util.List;

public class ComandaService {
    private final ComandaDao dao = new ComandaDao();
    private final ProductDao productDao = new ProductDao();
    private final ConfigService configService = new ConfigService();
    private final ComandaNumberService numberService = new ComandaNumberService();
    private final ConfigDao configDao = new ConfigDao();

    public void open(String cliente) {
        try {
            Comanda c = new Comanda();
            c.setNumero(numberService.nextNumber());
            c.setCliente(cliente);
            c.setAbertura(DateUtil.now());
            c.setStatus("ABERTA");
            c.setLimite(configService.defaultLimit());
            dao.save(c);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Comanda> find(String filter) { try { return dao.findByFilter(filter == null ? "" : filter); } catch (Exception e) { throw new RuntimeException(e); } }
    public List<ComandaItem> items(int comandaId) { try { return dao.listItems(comandaId); } catch (Exception e) { throw new RuntimeException(e); } }

    public void addItem(Comanda c, Product p, int qtd) {
        if (!"ABERTA".equals(c.getStatus())) throw new RuntimeException("Comanda nao esta ABERTA.");
        if (qtd <= 0) throw new RuntimeException("Quantidade invalida.");
        if (!p.isAtivo() || p.getEstoque() < qtd) throw new RuntimeException("Sem estoque suficiente.");
        double subtotal = p.getPreco() * qtd;
        double novoTotal = c.getTotal() + subtotal;
        if (novoTotal >= c.getLimite()) {
            try { dao.updateStatus(c.getId(), "BLOQUEADA"); } catch (Exception ignored) {}
            throw new RuntimeException("Limite atingido. Comanda bloqueada.");
        }
        try {
            dao.addItem(c.getId(), p.getId(), qtd, p.getPreco(), subtotal, SessionContext.getCurrentUser().getId(), DateUtil.now());
            productDao.changeStock(p.getId(), -qtd);
            dao.updateComandaTotals(c.getId());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void cancelItem(Comanda c, ComandaItem item) {
        if (!SessionContext.isAdmin()) throw new RuntimeException("Apenas ADMIN pode cancelar item.");
        try {
            dao.cancelItem(item.getId(), SessionContext.getCurrentUser().getId(), DateUtil.now());
            productDao.changeStock(item.getProdutoId(), item.getQuantidade());
            dao.updateComandaTotals(c.getId());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void applyDiscount(Comanda c, double discount, String reason) {
        if (!SessionContext.isAdmin()) throw new RuntimeException("Apenas ADMIN pode aplicar desconto.");
        if (reason == null || reason.isBlank()) throw new RuntimeException("Justificativa obrigatoria.");
        try { dao.updateDiscount(c.getId(), discount); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void close(Comanda c, String formaPagamento) {
        if (!"ABERTA".equals(c.getStatus()) && !"BLOQUEADA".equals(c.getStatus())) throw new RuntimeException("Comanda nao pode ser fechada.");
        try {
            double finalTotal = c.getTotal() - c.getDesconto();
            dao.savePayment(c.getId(), formaPagamento, c.getTotal(), c.getDesconto(), finalTotal, SessionContext.getCurrentUser().getNome(), DateUtil.now());
            dao.closeComanda(c.getId(), DateUtil.now());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void closeCaixa() {
        try {
            dao.clearCaixaData();
            configDao.save("numeracao_seq_atual", "0");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
