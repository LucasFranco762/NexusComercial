package nexuscomercial.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:nexuscomercial.db";

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usuarios (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  nome TEXT NOT NULL,
                  usuario TEXT NOT NULL UNIQUE,
                  senha TEXT NOT NULL,
                  perfil TEXT NOT NULL,
                  status TEXT NOT NULL
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS categorias (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  nome TEXT NOT NULL UNIQUE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS produtos (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  codigo TEXT NOT NULL UNIQUE,
                  nome TEXT NOT NULL,
                  categoria_id INTEGER,
                  preco REAL NOT NULL,
                  estoque INTEGER NOT NULL,
                  estoque_minimo INTEGER NOT NULL,
                  ativo INTEGER NOT NULL DEFAULT 1,
                  FOREIGN KEY(categoria_id) REFERENCES categorias(id)
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS comandas (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  numero TEXT NOT NULL UNIQUE,
                  cliente TEXT NOT NULL,
                  abertura TEXT NOT NULL,
                  fechamento TEXT,
                  status TEXT NOT NULL,
                  total REAL NOT NULL DEFAULT 0,
                  limite REAL NOT NULL,
                  desconto REAL NOT NULL DEFAULT 0
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS itens_comanda (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  comanda_id INTEGER NOT NULL,
                  produto_id INTEGER NOT NULL,
                  quantidade INTEGER NOT NULL,
                  valor_unitario REAL NOT NULL,
                  subtotal REAL NOT NULL,
                  usuario_id INTEGER NOT NULL,
                  lancamento TEXT NOT NULL,
                  cancelado INTEGER NOT NULL DEFAULT 0,
                  cancelado_por INTEGER,
                  cancelado_em TEXT,
                  FOREIGN KEY(comanda_id) REFERENCES comandas(id),
                  FOREIGN KEY(produto_id) REFERENCES produtos(id),
                  FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pagamentos (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  comanda_id INTEGER NOT NULL,
                  forma TEXT NOT NULL,
                  total_bruto REAL NOT NULL,
                  desconto REAL NOT NULL,
                  total_final REAL NOT NULL,
                  operador TEXT NOT NULL,
                  fechado_em TEXT NOT NULL,
                  FOREIGN KEY(comanda_id) REFERENCES comandas(id)
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS configuracoes (
                  chave TEXT PRIMARY KEY,
                  valor TEXT NOT NULL
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS historico_acoes (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  acao TEXT NOT NULL,
                  usuario TEXT NOT NULL,
                  data_hora TEXT NOT NULL,
                  detalhes TEXT
                )""");

            st.executeUpdate("INSERT OR IGNORE INTO configuracoes(chave,valor) VALUES('limite_padrao','200.00')");
            st.executeUpdate("INSERT OR IGNORE INTO configuracoes(chave,valor) VALUES('nome_estabelecimento','NexusComercial')");
            st.executeUpdate("INSERT OR IGNORE INTO configuracoes(chave,valor) VALUES('mensagem_recibo','Obrigado pela preferencia!')");
            st.executeUpdate("""
                INSERT OR IGNORE INTO usuarios(id,nome,usuario,senha,perfil,status)
                VALUES(1,'Administrador','admin','admin','ADMINISTRADOR','ATIVO')""");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco", e);
        }
    }
}
