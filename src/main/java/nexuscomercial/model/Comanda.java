package nexuscomercial.model;

public class Comanda {
    private int id;
    private String numero;
    private String cliente;
    private String abertura;
    private String fechamento;
    private String status;
    private double total;
    private double limite;
    private double desconto;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getAbertura() { return abertura; }
    public void setAbertura(String abertura) { this.abertura = abertura; }
    public String getFechamento() { return fechamento; }
    public void setFechamento(String fechamento) { this.fechamento = fechamento; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getLimite() { return limite; }
    public void setLimite(double limite) { this.limite = limite; }
    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; }

    @Override
    public String toString() { return "#" + numero + " - " + cliente + " (" + status + ")"; }
}
