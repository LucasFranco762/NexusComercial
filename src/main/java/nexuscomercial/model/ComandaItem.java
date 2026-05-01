package nexuscomercial.model;

public class ComandaItem {
    private int id;
    private int comandaId;
    private int produtoId;
    private String produtoNome;
    private int quantidade;
    private double valorUnitario;
    private double subtotal;
    private String usuarioNome;
    private String lancamento;
    private boolean cancelado;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getComandaId() { return comandaId; }
    public void setComandaId(int comandaId) { this.comandaId = comandaId; }
    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }
    public String getProdutoNome() { return produtoNome; }
    public void setProdutoNome(String produtoNome) { this.produtoNome = produtoNome; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public double getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(double valorUnitario) { this.valorUnitario = valorUnitario; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
    public String getLancamento() { return lancamento; }
    public void setLancamento(String lancamento) { this.lancamento = lancamento; }
    public boolean isCancelado() { return cancelado; }
    public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }
}
