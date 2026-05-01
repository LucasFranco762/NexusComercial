package nexuscomercial.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import nexuscomercial.model.*;
import nexuscomercial.service.*;
import nexuscomercial.util.AlertUtil;
import nexuscomercial.util.ReceiptUtil;
import nexuscomercial.util.SessionContext;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MainView {
    private final UserService userService = new UserService();
    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final ComandaService comandaService = new ComandaService();
    private final ReportService reportService = new ReportService();
    private final ConfigService configService = new ConfigService();
    private final TabPane tabPane = new TabPane();
    private final javafx.stage.Stage stage;

    public MainView(javafx.stage.Stage stage) { this.stage = stage; }

    public Parent build() {
        BorderPane root = new BorderPane();
        Label top = new Label("Usuário logado: " + SessionContext.getCurrentUser().getNome() + " [" + SessionContext.getCurrentUser().getPerfil() + "]");
        top.setPadding(new Insets(8));
        root.setTop(top);

        tabPane.getTabs().addAll(tabComandas(), tabVendas(), tabProdutosEstoque(), tabRelatorios(), tabConfiguracoes());
        if (SessionContext.isAdmin()) tabPane.getTabs().add(tabUsuarios());
        root.setCenter(tabPane);
        return root;
    }

    private Tab tabUsuarios() {
        Tab tab = new Tab("Usuários");
        TableView<User> table = new TableView<>();
        table.getColumns().add(col("Nome", u -> u.getNome()));
        table.getColumns().add(col("Usuário", User::getUsuario));
        table.getColumns().add(col("Perfil", User::getPerfil));
        table.getColumns().add(col("Status", User::getStatus));
        refreshUsers(table);

        TextField nome = new TextField(); TextField usuario = new TextField(); PasswordField senha = new PasswordField();
        ComboBox<String> perfil = new ComboBox<>(FXCollections.observableArrayList("ADMINISTRADOR", "OPERADOR")); perfil.setValue("OPERADOR");
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("ATIVO", "INATIVO")); status.setValue("ATIVO");
        Button save = new Button("Salvar/Atualizar");
        final int[] editId = {0};
        save.setOnAction(e -> {
            User u = new User(editId[0], nome.getText(), usuario.getText(), senha.getText(), perfil.getValue(), status.getValue());
            userService.save(u); editId[0] = 0; nome.clear(); usuario.clear(); senha.clear(); refreshUsers(table);
        });
        table.setOnMouseClicked(e -> {
            User u = table.getSelectionModel().getSelectedItem(); if (u == null) return;
            editId[0] = u.getId(); nome.setText(u.getNome()); usuario.setText(u.getUsuario()); senha.setText(u.getSenha()); perfil.setValue(u.getPerfil()); status.setValue(u.getStatus());
        });
        tab.setContent(vbox(table,
            labeledRow("Nome", nome),
            labeledRow("Usuário", usuario),
            labeledRow("Senha", senha),
            labeledRow("Perfil", perfil),
            labeledRow("Status", status),
            hbox(save)
        ));
        return tab;
    }

    private Tab tabProdutosEstoque() {
        Tab tab = new Tab("Produtos/Estoque");
        SplitPane split = new SplitPane();
        split.getItems().addAll(categoryPane(), productPane());
        split.setDividerPositions(0.32);
        tab.setContent(split);
        return tab;
    }

    private Parent categoryPane() {
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        Label t = new Label("Categorias");
        ListView<Category> list = new ListView<>();
        list.setItems(FXCollections.observableArrayList(categoryService.list()));
        TextField nome = new TextField();
        Button add = new Button("Adicionar");
        Button edit = new Button("Editar");
        Button del = new Button("Excluir");
        add.setOnAction(e -> { categoryService.save(nome.getText()); list.setItems(FXCollections.observableArrayList(categoryService.list())); nome.clear(); });
        edit.setOnAction(e -> { Category c = list.getSelectionModel().getSelectedItem(); if (c != null) { categoryService.update(c.getId(), nome.getText()); list.setItems(FXCollections.observableArrayList(categoryService.list())); }});
        del.setOnAction(e -> { Category c = list.getSelectionModel().getSelectedItem(); if (c != null) { categoryService.delete(c.getId()); list.setItems(FXCollections.observableArrayList(categoryService.list())); }});
        list.setOnMouseClicked(e -> { Category c = list.getSelectionModel().getSelectedItem(); if (c != null) nome.setText(c.getNome()); });
        box.getChildren().addAll(t, list, new Label("Nome"), nome, hbox(add, edit, del));
        return box;
    }

    private Parent productPane() {
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        TableView<Product> table = new TableView<>();
        table.getColumns().add(col("Código", Product::getCodigo));
        table.getColumns().add(col("Produto", Product::getNome));
        table.getColumns().add(col("Categoria", p -> p.getCategoriaNome() == null ? "" : p.getCategoriaNome()));
        table.getColumns().add(col("Preço", p -> String.format("%.2f", p.getPreco())));
        table.getColumns().add(col("Estoque", p -> String.valueOf(p.getEstoque())));
        table.getColumns().add(col("Mínimo", p -> String.valueOf(p.getEstoqueMinimo())));
        table.getColumns().add(col("Status", p -> p.isAtivo() ? "ATIVO" : "INATIVO"));
        refreshProducts(table);

        TextField codigo = new TextField(); TextField nome = new TextField(); TextField preco = new TextField();
        TextField estoque = new TextField(); TextField minimo = new TextField();
        ComboBox<Category> categoria = new ComboBox<>(FXCollections.observableArrayList(categoryService.list()));
        ComboBox<String> ativo = new ComboBox<>(FXCollections.observableArrayList("ATIVO", "INATIVO")); ativo.setValue("ATIVO");
        Button save = new Button("Salvar/Atualizar");
        final int[] editId = {0};
        save.setOnAction(e -> {
            Product p = new Product();
            p.setId(editId[0]); p.setCodigo(codigo.getText()); p.setNome(nome.getText());
            p.setCategoriaId(categoria.getValue() == null ? 0 : categoria.getValue().getId());
            p.setPreco(Double.parseDouble(preco.getText())); p.setEstoque(Integer.parseInt(estoque.getText()));
            p.setEstoqueMinimo(Integer.parseInt(minimo.getText())); p.setAtivo("ATIVO".equals(ativo.getValue()));
            productService.save(p); editId[0] = 0; refreshProducts(table);
        });
        table.setOnMouseClicked(e -> {
            Product p = table.getSelectionModel().getSelectedItem(); if (p == null) return;
            editId[0] = p.getId(); codigo.setText(p.getCodigo()); nome.setText(p.getNome()); preco.setText(String.valueOf(p.getPreco()));
            estoque.setText(String.valueOf(p.getEstoque())); minimo.setText(String.valueOf(p.getEstoqueMinimo())); ativo.setValue(p.isAtivo() ? "ATIVO" : "INATIVO");
        });
        box.getChildren().addAll(
            new Label("Produtos"), table,
            labeledRow("Código", codigo),
            labeledRow("Nome do Produto", nome),
            labeledRow("Categoria", categoria),
            labeledRow("Preço de Venda", preco),
            labeledRow("Quantidade em Estoque", estoque),
            labeledRow("Estoque Mínimo", minimo),
            labeledRow("Status", ativo),
            hbox(save)
        );
        return box;
    }

    private Tab tabComandas() {
        Tab tab = new Tab("Comandas");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        TextField numero = new TextField(); TextField cliente = new TextField();
        Button abrir = new Button("Abrir Comanda");
        TextField busca = new TextField();
        TableView<Comanda> table = new TableView<>();
        table.getColumns().add(col("Número", Comanda::getNumero));
        table.getColumns().add(col("Cliente", Comanda::getCliente));
        table.getColumns().add(col("Status", Comanda::getStatus));
        table.getColumns().add(col("Total", c -> String.format("%.2f", c.getTotal())));
        table.getColumns().add(col("Limite", c -> String.format("%.2f", c.getLimite())));
        refreshComandas(table, "");

        abrir.setOnAction(e -> { comandaService.open(numero.getText(), cliente.getText()); refreshComandas(table, ""); numero.clear(); cliente.clear(); });
        busca.textProperty().addListener((a,b,c) -> refreshComandas(table, c));
        box.getChildren().addAll(
            labeledRow("Número da Comanda", numero),
            labeledRow("Nome do Cliente", cliente),
            hbox(abrir),
            labeledRow("Busca por Número/Nome", busca),
            table
        );
        tab.setContent(box);
        return tab;
    }

    private Tab tabVendas() {
        Tab tab = new Tab("Vendas");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        ComboBox<Comanda> comanda = new ComboBox<>();
        comanda.setItems(FXCollections.observableArrayList(comandaService.find("")));
        ComboBox<Product> produto = new ComboBox<>();
        produto.setItems(FXCollections.observableArrayList(productService.listActiveInStock()));
        TextField qtd = new TextField("1");
        Button lancar = new Button("Lançar Item");
        TableView<ComandaItem> itens = new TableView<>();
        itens.getColumns().add(col("Produto", ComandaItem::getProdutoNome));
        itens.getColumns().add(col("Qtd", i -> String.valueOf(i.getQuantidade())));
        itens.getColumns().add(col("Unit", i -> String.format("%.2f", i.getValorUnitario())));
        itens.getColumns().add(col("Subtotal", i -> String.format("%.2f", i.getSubtotal())));
        itens.getColumns().add(col("Usuário", ComandaItem::getUsuarioNome));
        itens.getColumns().add(col("Status", i -> i.isCancelado() ? "CANCELADO" : "ATIVO"));
        Button cancelar = new Button("Cancelar Item (Admin)");
        Button desconto = new Button("Aplicar Desconto");
        TextField valorDesc = new TextField("0");
        TextField motivo = new TextField();
        ComboBox<String> pgto = new ComboBox<>(FXCollections.observableArrayList("Dinheiro", "Pix", "Cartão de Débito", "Cartão de Crédito", "Outro"));
        pgto.setValue("Dinheiro");
        Button fechar = new Button("Fechar Comanda");

        comanda.setOnAction(e -> { Comanda c = comanda.getValue(); if (c != null) itens.setItems(FXCollections.observableArrayList(comandaService.items(c.getId()))); });
        lancar.setOnAction(e -> {
            Comanda c = comanda.getValue(); Product p = produto.getValue(); if (c == null || p == null) return;
            comandaService.addItem(c, p, Integer.parseInt(qtd.getText())); refreshComandaAndItems(comanda, produto, itens);
        });
        cancelar.setOnAction(e -> {
            Comanda c = comanda.getValue(); ComandaItem i = itens.getSelectionModel().getSelectedItem(); if (c == null || i == null) return;
            comandaService.cancelItem(c, i); refreshComandaAndItems(comanda, produto, itens);
        });
        desconto.setOnAction(e -> {
            Comanda c = comanda.getValue(); if (c == null) return;
            comandaService.applyDiscount(c, Double.parseDouble(valorDesc.getText()), motivo.getText()); refreshComandaAndItems(comanda, produto, itens);
        });
        fechar.setOnAction(e -> {
            Comanda c = comanda.getValue(); if (c == null) return;
            comandaService.close(c, pgto.getValue());
            var cfg = configService.getAll();
            var path = ReceiptUtil.generate(c, comandaService.items(c.getId()), pgto.getValue(), SessionContext.getCurrentUser().getNome(), cfg);
            AlertUtil.info("Comanda fechada. Recibo: " + path.toAbsolutePath());
            refreshComandaAndItems(comanda, produto, itens);
        });
        box.getChildren().addAll(
            labeledRow("Comanda", comanda),
            labeledRow("Produto", produto),
            labeledRow("Quantidade", qtd),
            hbox(lancar),
            itens,
            hbox(cancelar, new Label("Desconto"), valorDesc, new Label("Justificativa"), motivo, desconto),
            hbox(new Label("Forma de Pagamento"), pgto, fechar)
        );
        tab.setContent(box);
        return tab;
    }

    private Tab tabRelatorios() {
        Tab tab = new Tab("Relatórios");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        DatePicker data = new DatePicker(LocalDate.now());
        Button diario = new Button("Gerar Diário");
        TextArea out = new TextArea();
        Button estoqueBaixo = new Button("Estoque Baixo");
        TextField filtro = new TextField();
        Button historico = new Button("Histórico Fechadas");

        diario.setOnAction(e -> out.setText(reportService.dailyReport(data.getValue().toString())));
        estoqueBaixo.setOnAction(e -> {
            StringBuilder sb = new StringBuilder("Estoque baixo:\n");
            for (Product p : productService.lowStock()) sb.append("- ").append(p.getNome()).append(" | atual: ").append(p.getEstoque()).append(" | mínimo: ").append(p.getEstoqueMinimo()).append("\n");
            out.setText(sb.toString());
        });
        historico.setOnAction(e -> out.setText(String.join("\n", reportService.closedComandas(filtro.getText()))));
        box.getChildren().addAll(hbox(new Label("Data"), data, diario, estoqueBaixo), hbox(new Label("Filtro histórico"), filtro, historico), out);
        tab.setContent(box);
        return tab;
    }

    private Tab tabConfiguracoes() {
        Tab tab = new Tab("Configurações");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        Map<String, String> cfg = configService.getAll();
        TextField limite = new TextField(cfg.getOrDefault("limite_padrao", "200"));
        TextField nome = new TextField(cfg.getOrDefault("nome_estabelecimento", "NexusComercial"));
        TextField msg = new TextField(cfg.getOrDefault("mensagem_recibo", ""));
        Button save = new Button("Salvar Configurações");
        save.setOnAction(e -> {
            Map<String, String> map = new HashMap<>();
            map.put("limite_padrao", limite.getText());
            map.put("nome_estabelecimento", nome.getText());
            map.put("mensagem_recibo", msg.getText());
            configService.save(map);
            AlertUtil.info("Configurações salvas.");
        });
        box.getChildren().addAll(new Label("Limite padrão da comanda"), limite, new Label("Nome do estabelecimento"), nome, new Label("Mensagem do recibo"), msg, save);
        tab.setContent(box);
        return tab;
    }

    private <T> TableColumn<T, String> col(String title, java.util.function.Function<T, String> mapper) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(v -> new SimpleStringProperty(mapper.apply(v.getValue())));
        c.setPrefWidth(140);
        return c;
    }

    private HBox hbox(javafx.scene.Node... n) { HBox h = new HBox(8, n); return h; }
    private VBox vbox(javafx.scene.Node... n) { VBox v = new VBox(8, n); v.setPadding(new Insets(10)); return v; }
    private HBox labeledRow(String labelText, javafx.scene.Node input) {
        Label label = new Label(labelText + ":");
        label.setPrefWidth(190);
        HBox row = new HBox(8, label, input);
        HBox.setHgrow(input, Priority.ALWAYS);
        return row;
    }
    private void refreshUsers(TableView<User> t) { t.setItems(FXCollections.observableArrayList(userService.list())); }
    private void refreshProducts(TableView<Product> t) { t.setItems(FXCollections.observableArrayList(productService.list())); }
    private void refreshComandas(TableView<Comanda> t, String f) { t.setItems(FXCollections.observableArrayList(comandaService.find(f))); }
    private void refreshComandaAndItems(ComboBox<Comanda> com, ComboBox<Product> prod, TableView<ComandaItem> itens) {
        Comanda selected = com.getValue();
        com.setItems(FXCollections.observableArrayList(comandaService.find("")));
        prod.setItems(FXCollections.observableArrayList(productService.listActiveInStock()));
        if (selected != null) {
            for (Comanda c : com.getItems()) if (c.getId() == selected.getId()) com.setValue(c);
            if (com.getValue() != null) itens.setItems(FXCollections.observableArrayList(comandaService.items(com.getValue().getId())));
        }
    }
}
