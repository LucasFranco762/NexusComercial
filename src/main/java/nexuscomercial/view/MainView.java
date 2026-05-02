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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class MainView {
    private final UserService userService = new UserService();
    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final ComandaService comandaService = new ComandaService();
    private final ReportService reportService = new ReportService();
    private final ConfigService configService = new ConfigService();
    private final ComandaNumberService comandaNumberService = new ComandaNumberService();
    private final TabPane tabPane = new TabPane();
    private final javafx.stage.Stage stage;
    private TableView<Comanda> comandasTable;
    private TextField comandasBuscaField;
    private CheckBox comandasAbertaCheck;
    private CheckBox comandasFechadaCheck;

    public MainView(javafx.stage.Stage stage) { this.stage = stage; }

    public Parent build() {
        BorderPane root = new BorderPane();
        Label top = new Label("Usuario logado: " + SessionContext.getCurrentUser().getNome() + " [" + SessionContext.getCurrentUser().getPerfil() + "]");
        top.setPadding(new Insets(8));
        root.setTop(top);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(tabComandas(), tabVendas(), tabCaixa(), tabProdutosEstoque(), tabRelatorios(), tabConfiguracoes());
        if (SessionContext.isAdmin()) tabPane.getTabs().add(tabUsuarios());
        root.setCenter(tabPane);
        return root;
    }

    private Tab tabUsuarios() {
        Tab tab = new Tab("Usuarios");
        TableView<User> table = new TableView<>();
        table.getColumns().add(col("Nome", User::getNome));
        table.getColumns().add(col("Usuario", User::getUsuario));
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
            userService.save(u);
            editId[0] = 0; nome.clear(); usuario.clear(); senha.clear(); refreshUsers(table);
        });
        table.setOnMouseClicked(e -> {
            User u = table.getSelectionModel().getSelectedItem(); if (u == null) return;
            editId[0] = u.getId(); nome.setText(u.getNome()); usuario.setText(u.getUsuario()); senha.setText(u.getSenha()); perfil.setValue(u.getPerfil()); status.setValue(u.getStatus());
        });
        tab.setContent(vbox(table, labeledRow("Nome", nome), labeledRow("Usuario", usuario), labeledRow("Senha", senha), labeledRow("Perfil", perfil), labeledRow("Status", status), hbox(save)));
        return tab;
    }

    private Tab tabProdutosEstoque() {
        Tab tab = new Tab("Produtos/Estoque");
        tab.setContent(productPane());
        return tab;
    }

    private Parent productPane() {
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        TableView<Product> table = new TableView<>();
        table.getColumns().add(col("Codigo", Product::getCodigo));
        table.getColumns().add(col("Produto", Product::getNome));
        table.getColumns().add(col("Categoria", p -> p.getCategoriaNome() == null ? "" : p.getCategoriaNome()));
        table.getColumns().add(col("Preco", p -> String.format("%.2f", p.getPreco())));
        table.getColumns().add(col("Estoque", p -> String.valueOf(p.getEstoque())));
        table.getColumns().add(col("Minimo", p -> String.valueOf(p.getEstoqueMinimo())));
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
            productService.save(p);
            editId[0] = 0;
            categoria.setItems(FXCollections.observableArrayList(categoryService.list()));
            refreshProducts(table);
        });
        table.setOnMouseClicked(e -> {
            Product p = table.getSelectionModel().getSelectedItem(); if (p == null) return;
            editId[0] = p.getId(); codigo.setText(p.getCodigo()); nome.setText(p.getNome()); preco.setText(String.valueOf(p.getPreco()));
            estoque.setText(String.valueOf(p.getEstoque())); minimo.setText(String.valueOf(p.getEstoqueMinimo())); ativo.setValue(p.isAtivo() ? "ATIVO" : "INATIVO");
        });
        box.getChildren().addAll(
            new Label("Produtos"), table,
            labeledRow("Codigo", codigo),
            labeledRow("Nome do Produto", nome),
            labeledRow("Categoria", categoria),
            labeledRow("Preco de Venda", preco),
            labeledRow("Quantidade em Estoque", estoque),
            labeledRow("Estoque Minimo", minimo),
            labeledRow("Status", ativo),
            hbox(save)
        );
        return box;
    }

    private Tab tabComandas() {
        Tab tab = new Tab("Comandas");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        TextField numero = new TextField(comandaNumberService.peekNextNumber());
        numero.setEditable(false);
        numero.setDisable(true);
        numero.setPrefColumnCount(6);
        setFieldWidthByChars(numero, 6);
        TextField cliente = new TextField();
        UnaryOperator<TextFormatter.Change> clienteFilter = change -> {
            String novoTexto = change.getControlNewText();
            if (novoTexto.length() > 50) return null;
            return change;
        };
        cliente.setTextFormatter(new TextFormatter<>(clienteFilter));
        cliente.setPrefColumnCount(50);
        setFieldWidthByChars(cliente, 50);
        final boolean[] updatingCliente = {false};
        cliente.textProperty().addListener((obs, oldV, newV) -> {
            if (updatingCliente[0]) return;
            String title = toTitleCase(newV);
            if (!title.equals(newV)) {
                updatingCliente[0] = true;
                int pos = cliente.getCaretPosition();
                cliente.setText(title);
                cliente.positionCaret(Math.min(pos, title.length()));
                updatingCliente[0] = false;
            }
        });
        Button abrir = new Button("Abrir Comanda");
        abrir.setDisable(true);
        cliente.textProperty().addListener((obs, oldV, newV) -> abrir.setDisable(newV == null || newV.isBlank()));
        TextField busca = new TextField();
        CheckBox abertaCheck = new CheckBox("Aberta");
        CheckBox fechadaCheck = new CheckBox("Fechada");
        abertaCheck.setSelected(true);
        fechadaCheck.setSelected(true);
        this.comandasAbertaCheck = abertaCheck;
        this.comandasFechadaCheck = fechadaCheck;
        TableView<Comanda> table = new TableView<>();
        this.comandasBuscaField = busca;
        this.comandasTable = table;
        table.getColumns().add(col("Numero", Comanda::getNumero));
        table.getColumns().add(col("Cliente", Comanda::getCliente));
        table.getColumns().add(col("Status", Comanda::getStatus));
        table.getColumns().add(col("Total", c -> String.format("%.2f", c.getTotal())));
        table.getColumns().add(col("Limite", c -> String.format("%.2f", c.getLimite())));
        refreshComandas(table, "");

        abrir.setOnAction(e -> {
            comandaService.open(cliente.getText());
            refreshComandas(table, "");
            cliente.clear();
            numero.setText(comandaNumberService.peekNextNumber());
        });
        busca.textProperty().addListener((a, b, c) -> refreshComandas(table, c));
        abertaCheck.setOnAction(e -> refreshComandas(table, busca.getText()));
        fechadaCheck.setOnAction(e -> refreshComandas(table, busca.getText()));
        HBox buscaRow = labeledRow("Busca por Numero/Nome", busca);
        VBox.setMargin(buscaRow, new Insets(14, 0, 0, 0));
        HBox actionRow = hbox(abrir, spacer(120), abertaCheck, spacer(20), fechadaCheck);
        VBox.setMargin(actionRow, new Insets(12, 0, 0, 0));
        box.getChildren().addAll(
            labeledRowFixed("Numero da Comanda", numero, 130),
            labeledRowFixed("Nome do Cliente", cliente, 130),
            actionRow,
            buscaRow,
            table
        );
        tab.setContent(box);
        return tab;
    }

    private Tab tabVendas() {
        Tab tab = new Tab("Vendas");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        ComboBox<Comanda> comanda = new ComboBox<>();
        comanda.setItems(FXCollections.observableArrayList(openComandas()));
        ComboBox<Product> produto = new ComboBox<>();
        produto.setItems(FXCollections.observableArrayList(productService.listActiveInStock()));
        TextField qtd = new TextField("1");
        Button lancar = new Button("Lancar Item");
        TableView<ComandaItem> itens = new TableView<>();
        itens.getColumns().add(col("Produto", ComandaItem::getProdutoNome));
        itens.getColumns().add(col("Qtd", i -> String.valueOf(i.getQuantidade())));
        itens.getColumns().add(col("Unit", i -> String.format("%.2f", i.getValorUnitario())));
        itens.getColumns().add(col("Subtotal", i -> String.format("%.2f", i.getSubtotal())));
        itens.getColumns().add(col("Usuario", ComandaItem::getUsuarioNome));
        itens.getColumns().add(col("Status", i -> i.isCancelado() ? "CANCELADO" : "ATIVO"));
        Button cancelar = new Button("Cancelar Item (Admin)");
        Button desconto = new Button("Aplicar Desconto");
        TextField valorDesc = new TextField("0");
        TextField motivo = new TextField();
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
        VBox.setMargin(itens, new Insets(14, 0, 0, 0));
        box.getChildren().addAll(
            labeledRowFixed("Comanda", comanda, 130),
            labeledRowFixed("Produto", produto, 130),
            labeledRowFixed("Quantidade", qtd, 130),
            hbox(lancar),
            itens,
            hbox(cancelar, new Label("Desconto"), valorDesc, new Label("Justificativa"), motivo, desconto)
        );
        tab.setContent(box);
        return tab;
    }

    private Tab tabCaixa() {
        Tab tab = new Tab("Caixa");
        VBox box = new VBox(8); box.setPadding(new Insets(10));

        ComboBox<Comanda> comanda = new ComboBox<>();
        ComboBox<String> pgto = new ComboBox<>(FXCollections.observableArrayList("Dinheiro", "Pix", "Cartao de Debito", "Cartao de Credito", "Outro"));
        pgto.setValue("Dinheiro");
        Button fechar = new Button("Fechar Comanda");
        ListView<String> listaFechadas = new ListView<>();
        Label totalVendido = new Label();
        Label comandasAbertas = new Label();
        Label comandasFechadas = new Label();
        Label comandaMaior = new Label();
        Label comandaMenor = new Label();
        Label lucroLiquido = new Label();
        ListView<String> formasPagamento = new ListView<>();
        ListView<String> itensVendidos = new ListView<>();
        Label itemMaisVendido = new Label();
        Label itemMenosVendido = new Label();
        formasPagamento.setPrefHeight(150);
        itensVendidos.setPrefHeight(150);

        Runnable refreshCaixa = () -> {
            comanda.setItems(FXCollections.observableArrayList(comandaService.find("").stream()
                .filter(c -> "ABERTA".equals(c.getStatus()) || "BLOQUEADA".equals(c.getStatus()))
                .toList()));
            listaFechadas.setItems(FXCollections.observableArrayList(reportService.closedComandaValues()));
            Map<String, String> resumo = reportService.caixaResumo();
            totalVendido.setText("Total vendido: " + resumo.get("totalVendido"));
            comandasAbertas.setText("Comandas em aberto: " + resumo.get("comandasAbertas"));
            comandasFechadas.setText("Comandas fechadas: " + resumo.get("comandasFechadas"));
            comandaMaior.setText("Comanda com maior valor: " + resumo.get("comandaMaior"));
            comandaMenor.setText("Comanda com menor valor: " + resumo.get("comandaMenor"));
            lucroLiquido.setText("Lucro liquido: " + resumo.get("lucroLiquido"));
            formasPagamento.setItems(FXCollections.observableArrayList(reportService.resumoPorFormaPagamento()));
            itensVendidos.setItems(FXCollections.observableArrayList(reportService.resumoItensVendidos()));
            itemMaisVendido.setText("Item mais vendido: " + reportService.itemMaisVendido());
            itemMenosVendido.setText("Item menos vendido: " + reportService.itemMenosVendido());
        };
        refreshCaixa.run();

        fechar.setOnAction(e -> {
            Comanda c = comanda.getValue(); if (c == null) return;
            comandaService.close(c, pgto.getValue());
            var cfg = configService.getAll();
            var path = ReceiptUtil.generate(c, comandaService.items(c.getId()), pgto.getValue(), SessionContext.getCurrentUser().getNome(), cfg);
            AlertUtil.info("Comanda fechada. Recibo: " + path.toAbsolutePath());
            refreshCaixa.run();
            refreshComandasInstant();
        });
        Separator caixaSeparator = new Separator();
        VBox.setMargin(caixaSeparator, new Insets(14, 0, 0, 0));
        VBox leftPane = new VBox(8,
            labeledRow("Comanda para Fechamento", comanda),
            labeledRow("Forma de Pagamento", pgto),
            hbox(fechar),
            caixaSeparator,
            new Label("Lista de Comandas Fechadas (Valores)"),
            listaFechadas
        );
        GridPane cardsGrid = new GridPane();
        cardsGrid.setHgap(12);
        cardsGrid.setVgap(12);
        cardsGrid.add(createCard("Total vendido", totalVendido), 0, 0);
        cardsGrid.add(createCard("Comandas em aberto", comandasAbertas), 1, 0);
        cardsGrid.add(createCard("Comandas fechadas", comandasFechadas), 2, 0);
        cardsGrid.add(createCard("Comanda maior valor", comandaMaior), 0, 1);
        cardsGrid.add(createCard("Comanda menor valor", comandaMenor), 1, 1);
        cardsGrid.add(createCard("Lucro liquido", lucroLiquido), 2, 1);
        cardsGrid.add(createCard("Forma de pagamento", formasPagamento), 0, 2);
        cardsGrid.add(createCard("Itens vendidos", itensVendidos), 1, 2);
        cardsGrid.add(createCard("Item mais vendido", itemMaisVendido), 2, 2);
        cardsGrid.add(createCard("Item menos vendido", itemMenosVendido), 0, 3);

        VBox rightPane = new VBox(10, cardsGrid);
        ScrollPane rightScroll = new ScrollPane(rightPane);
        rightScroll.setFitToWidth(true);
        SplitPane split = new SplitPane(leftPane, rightScroll);
        split.setDividerPositions(0.20);
        VBox.setVgrow(split, Priority.ALWAYS);
        box.getChildren().add(split);
        tab.setContent(box);
        return tab;
    }

    private Tab tabRelatorios() {
        Tab tab = new Tab("Relatorios");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        DatePicker data = new DatePicker(LocalDate.now());
        Button diario = new Button("Gerar Diario");
        TextArea out = new TextArea();
        Button estoqueBaixo = new Button("Estoque Baixo");
        TextField filtro = new TextField();
        Button historico = new Button("Historico Fechadas");

        diario.setOnAction(e -> out.setText(reportService.dailyReport(data.getValue().toString())));
        estoqueBaixo.setOnAction(e -> {
            StringBuilder sb = new StringBuilder("Estoque baixo:\n");
            for (Product p : productService.lowStock()) sb.append("- ").append(p.getNome()).append(" | atual: ").append(p.getEstoque()).append(" | minimo: ").append(p.getEstoqueMinimo()).append("\n");
            out.setText(sb.toString());
        });
        historico.setOnAction(e -> out.setText(String.join("\n", reportService.closedComandas(filtro.getText()))));
        box.getChildren().addAll(hbox(new Label("Data"), data, diario, estoqueBaixo), hbox(new Label("Filtro historico"), filtro, historico), out);
        tab.setContent(box);
        return tab;
    }

    private Tab tabConfiguracoes() {
        Tab tab = new Tab("Configuracoes");
        VBox box = new VBox(8); box.setPadding(new Insets(10));
        Map<String, String> cfg = configService.getAll();
        TextField limite = new TextField(cfg.getOrDefault("limite_padrao", "200"));
        TextField nome = new TextField(cfg.getOrDefault("nome_estabelecimento", "NexusComercial"));
        TextField msg = new TextField(cfg.getOrDefault("mensagem_recibo", ""));
        ComboBox<String> numeracaoModo = new ComboBox<>(FXCollections.observableArrayList(
            Arrays.asList("FIM_DOS_NUMEROS", "DIARIO", "SEMANAL", "MENSAL", "ANUAL", "PERSONALIZADO")
        ));
        numeracaoModo.setValue(cfg.getOrDefault("numeracao_modo", "FIM_DOS_NUMEROS"));
        DatePicker dataPersonalizada = new DatePicker();
        String dataCustom = cfg.getOrDefault("numeracao_data_personalizada", "");
        if (!dataCustom.isBlank()) dataPersonalizada.setValue(LocalDate.parse(dataCustom));
        dataPersonalizada.setDisable(!"PERSONALIZADO".equals(numeracaoModo.getValue()));
        numeracaoModo.setOnAction(e -> dataPersonalizada.setDisable(!"PERSONALIZADO".equals(numeracaoModo.getValue())));
        Button save = new Button("Salvar Configuracoes");
        save.setOnAction(e -> {
            Map<String, String> map = new HashMap<>();
            map.put("limite_padrao", limite.getText());
            map.put("nome_estabelecimento", nome.getText());
            map.put("mensagem_recibo", msg.getText());
            map.put("numeracao_modo", numeracaoModo.getValue());
            map.put("numeracao_data_personalizada", dataPersonalizada.getValue() == null ? "" : dataPersonalizada.getValue().toString());
            configService.save(map);
            AlertUtil.info("Configuracoes salvas.");
        });

        Label catTitle = new Label("Categorias de Produtos");
        ListView<Category> catList = new ListView<>(FXCollections.observableArrayList(categoryService.list()));
        TextField catNome = new TextField();
        Button catAdd = new Button("Adicionar");
        Button catEdit = new Button("Editar");
        Button catDelete = new Button("Excluir");
        final int[] catEditId = {0};
        catList.setOnMouseClicked(e -> {
            Category c = catList.getSelectionModel().getSelectedItem();
            if (c != null) {
                catEditId[0] = c.getId();
                catNome.setText(c.getNome());
            }
        });
        catAdd.setOnAction(e -> {
            categoryService.save(catNome.getText());
            catEditId[0] = 0; catNome.clear();
            catList.setItems(FXCollections.observableArrayList(categoryService.list()));
        });
        catEdit.setOnAction(e -> {
            if (catEditId[0] == 0) return;
            categoryService.update(catEditId[0], catNome.getText());
            catEditId[0] = 0; catNome.clear();
            catList.setItems(FXCollections.observableArrayList(categoryService.list()));
        });
        catDelete.setOnAction(e -> {
            Category c = catList.getSelectionModel().getSelectedItem(); if (c == null) return;
            categoryService.delete(c.getId());
            catEditId[0] = 0; catNome.clear();
            catList.setItems(FXCollections.observableArrayList(categoryService.list()));
        });

        box.getChildren().addAll(
            labeledRow("Limite padrao da comanda", limite),
            labeledRow("Nome do estabelecimento", nome),
            labeledRow("Mensagem do recibo", msg),
            labeledRow("Reinicio da Numeracao", numeracaoModo),
            labeledRow("Data Personalizada", dataPersonalizada),
            hbox(save),
            new Separator(),
            catTitle,
            catList,
            labeledRow("Nome da categoria", catNome),
            hbox(catAdd, catEdit, catDelete)
        );
        tab.setContent(box);
        return tab;
    }

    private <T> TableColumn<T, String> col(String title, java.util.function.Function<T, String> mapper) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(v -> new SimpleStringProperty(mapper.apply(v.getValue())));
        c.setPrefWidth(140);
        return c;
    }

    private HBox hbox(javafx.scene.Node... n) { return new HBox(8, n); }
    private VBox vbox(javafx.scene.Node... n) { VBox v = new VBox(8, n); v.setPadding(new Insets(10)); return v; }
    private HBox labeledRow(String labelText, javafx.scene.Node input) {
        Label label = new Label(labelText + ":");
        label.setPrefWidth(190);
        HBox row = new HBox(8, label, input);
        HBox.setHgrow(input, Priority.ALWAYS);
        return row;
    }

    private HBox labeledRow(String labelText, javafx.scene.Node input, double labelWidth) {
        Label label = new Label(labelText + ":");
        label.setPrefWidth(labelWidth);
        HBox row = new HBox(8, label, input);
        HBox.setHgrow(input, Priority.ALWAYS);
        return row;
    }

    private HBox labeledRowFixed(String labelText, javafx.scene.Node input, double labelWidth) {
        Label label = new Label(labelText + ":");
        label.setPrefWidth(labelWidth);
        return new HBox(8, label, input);
    }

    private void refreshUsers(TableView<User> t) { t.setItems(FXCollections.observableArrayList(userService.list())); }
    private void refreshProducts(TableView<Product> t) { t.setItems(FXCollections.observableArrayList(productService.list())); }
    private void refreshComandas(TableView<Comanda> t, String f) {
        boolean showAbertas = comandasAbertaCheck == null || comandasAbertaCheck.isSelected();
        boolean showFechadas = comandasFechadaCheck == null || comandasFechadaCheck.isSelected();
        t.setItems(FXCollections.observableArrayList(
            comandaService.find(f).stream().filter(c -> {
                if ("FECHADA".equals(c.getStatus())) return showFechadas;
                return showAbertas;
            }).toList()
        ));
    }
    private void refreshComandaAndItems(ComboBox<Comanda> com, ComboBox<Product> prod, TableView<ComandaItem> itens) {
        Comanda selected = com.getValue();
        com.setItems(FXCollections.observableArrayList(openComandas()));
        prod.setItems(FXCollections.observableArrayList(productService.listActiveInStock()));
        if (selected != null) {
            for (Comanda c : com.getItems()) if (c.getId() == selected.getId()) com.setValue(c);
            if (com.getValue() != null) itens.setItems(FXCollections.observableArrayList(comandaService.items(com.getValue().getId())));
        }
    }

    private void refreshComandasInstant() {
        if (comandasTable == null) return;
        String filtro = comandasBuscaField == null ? "" : comandasBuscaField.getText();
        refreshComandas(comandasTable, filtro);
    }

    private String toTitleCase(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        StringBuilder sb = new StringBuilder(text.length());
        boolean capitalizeNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                sb.append(ch);
                capitalizeNext = true;
            } else {
                sb.append(capitalizeNext ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                capitalizeNext = false;
            }
        }
        return sb.toString();
    }

    private void setFieldWidthByChars(TextField field, int chars) {
        double width = chars * 9.0 + 26.0;
        field.setPrefWidth(width);
        field.setMinWidth(width);
        field.setMaxWidth(width);
    }

    private java.util.List<Comanda> openComandas() {
        return comandaService.find("").stream().filter(c -> "ABERTA".equals(c.getStatus())).toList();
    }

    private Region spacer(double width) {
        Region r = new Region();
        r.setMinWidth(width);
        r.setPrefWidth(width);
        r.setMaxWidth(width);
        return r;
    }

    private VBox createCard(String title, javafx.scene.Node... content) {
        Label t = new Label(title);
        VBox card = new VBox(6);
        card.getChildren().add(t);
        card.getChildren().addAll(content);
        card.setPadding(new Insets(10));
        card.setPrefSize(200, 200);
        card.setMinSize(200, 200);
        card.setMaxSize(200, 200);
        card.setStyle("-fx-background-color: #2A2A2A; -fx-border-color: #3A3A3A; -fx-border-radius: 6; -fx-background-radius: 6;");
        return card;
    }
}
