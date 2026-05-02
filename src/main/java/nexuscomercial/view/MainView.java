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
import nexuscomercial.util.ConfigJsonStore;
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
    private final Map<String, Double> persistedColumnWidths = new HashMap<>(ConfigJsonStore.loadColumnWidths());
    private final javafx.stage.Stage stage;
    private TableView<Comanda> comandasTable;
    private TextField comandasBuscaField;
    private TextField comandaNumeroPreviewField;
    private CheckBox comandasAbertaCheck;
    private CheckBox comandasFechadaCheck;
    private Runnable caixaRefreshAction = () -> {};

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
        this.comandaNumeroPreviewField = numero;
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
        Button detalhar = new Button("Detalhar");
        detalhar.setDisable(true);
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
            refreshNumeroComandaPreview();
            caixaRefreshAction.run();
        });
        busca.textProperty().addListener((a, b, c) -> refreshComandas(table, c));
        abertaCheck.setOnAction(e -> refreshComandas(table, busca.getText()));
        fechadaCheck.setOnAction(e -> refreshComandas(table, busca.getText()));
        HBox buscaRow = labeledRow("Busca por Numero/Nome", busca);
        VBox.setMargin(buscaRow, new Insets(14, 0, 0, 0));
        HBox actionRow = hbox(abrir, spacer(120), abertaCheck, spacer(20), fechadaCheck, spacer(100), detalhar);
        VBox.setMargin(actionRow, new Insets(12, 0, 0, 0));

        Label detailTitle = new Label("Comanda -");
        Label detailCliente = new Label("Nome do Cliente: -");
        TableView<ComandaItem> detailItens = new TableView<>();
        detailItens.getColumns().add(col("QTD", i -> String.valueOf(i.getQuantidade())));
        detailItens.getColumns().add(col("Item", ComandaItem::getProdutoNome));
        detailItens.getColumns().add(col("Valor unitario", i -> String.format("R$ %.2f", i.getValorUnitario())));
        detailItens.getColumns().add(col("Sub-total", i -> String.format("R$ %.2f", i.getSubtotal())));
        detailItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Label detailLimite = new Label("Limite: R$ 0.00");
        Label detailTotal = new Label("Total: R$ 0.00");
        Label detailRestante = new Label("Restante para limite: R$ 0.00");

        java.util.function.Consumer<Comanda> renderDetail = c -> {
            if (c == null) return;
            detailTitle.setText("Comanda " + c.getNumero());
            detailCliente.setText("Nome do Cliente: " + c.getCliente());
            detailItens.setItems(FXCollections.observableArrayList(comandaService.items(c.getId()).stream().filter(i -> !i.isCancelado()).toList()));
            detailLimite.setText("Limite: R$ " + String.format("%.2f", c.getLimite()));
            detailTotal.setText("Total: R$ " + String.format("%.2f", c.getTotal()));
            double restante = c.getLimite() - c.getTotal();
            detailRestante.setText("Restante para limite: R$ " + String.format("%.2f", restante));
        };
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> detalhar.setDisable(selected == null));
        detalhar.setOnAction(e -> renderDetail.accept(table.getSelectionModel().getSelectedItem()));
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;
            Comanda c = table.getSelectionModel().getSelectedItem();
            renderDetail.accept(c);
        });

        VBox rightDetail = new VBox(8, detailTitle, detailCliente, detailItens, detailLimite, detailTotal, detailRestante);
        VBox.setVgrow(detailItens, Priority.ALWAYS);
        rightDetail.setPadding(new Insets(0, 0, 0, 10));
        SplitPane lowerSplit = new SplitPane(table, rightDetail);
        lowerSplit.setDividerPositions(0.58);
        VBox.setVgrow(lowerSplit, Priority.ALWAYS);
        Separator blueDivider = new Separator();
        blueDivider.setStyle("-fx-background-color: #007BFF; -fx-border-color: #007BFF;");

        box.getChildren().addAll(
            labeledRowFixed("Numero da Comanda", numero, 130),
            labeledRowFixed("Nome do Cliente", cliente, 130),
            actionRow,
            buscaRow,
            blueDivider,
            lowerSplit
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
        Button fecharCaixa = new Button("FECHAR CAIXA");
        fecharCaixa.setStyle("-fx-background-color: #DC3545; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        TableView<Comanda> tabelaFechadas = new TableView<>();
        tabelaFechadas.getColumns().add(col("Numero", Comanda::getNumero));
        tabelaFechadas.getColumns().add(col("Cliente", Comanda::getCliente));
        tabelaFechadas.getColumns().add(col("Valor", c -> String.format("R$ %.2f", c.getTotal() - c.getDesconto())));
        tabelaFechadas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        bindColumnWidthPersistence(tabelaFechadas, "caixa_fechadas");
        Label totalVendido = new Label();
        Label comandasAbertas = new Label();
        Label comandasFechadas = new Label();
        Label comandaMaior = new Label();
        Label comandaMenor = new Label();
        Label lucroLiquido = new Label();
        Label formasPagamento = new Label();
        Label itensVendidos = new Label();
        Label itemMaisVendido = new Label();
        Label itemMenosVendido = new Label();
        formasPagamento.setWrapText(true);
        itensVendidos.setWrapText(true);
        ScrollPane formasPagamentoScroll = new ScrollPane(formasPagamento);
        formasPagamentoScroll.setFitToWidth(true);
        formasPagamentoScroll.setPrefViewportHeight(145);
        formasPagamentoScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        ScrollPane itensVendidosScroll = new ScrollPane(itensVendidos);
        itensVendidosScroll.setFitToWidth(true);
        itensVendidosScroll.setPrefViewportHeight(145);
        itensVendidosScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Runnable refreshCaixa = () -> {
            comanda.setItems(FXCollections.observableArrayList(comandaService.find("").stream()
                .filter(c -> "ABERTA".equals(c.getStatus()) || "BLOQUEADA".equals(c.getStatus()))
                .toList()));
            tabelaFechadas.setItems(FXCollections.observableArrayList(
                comandaService.find("").stream().filter(c -> "FECHADA".equals(c.getStatus())).toList()
            ));
            Map<String, String> resumo = reportService.caixaResumo();
            totalVendido.setText("R$ " + resumo.get("totalVendido").replace("R$ ", ""));
            comandasAbertas.setText(resumo.get("comandasAbertas"));
            comandasFechadas.setText(resumo.get("comandasFechadas"));
            comandaMaior.setText(resumo.get("comandaMaior"));
            comandaMenor.setText(resumo.get("comandaMenor"));
            lucroLiquido.setText(resumo.get("lucroLiquido"));
            formasPagamento.setText(String.join("\n", reportService.resumoPorFormaPagamento()));
            itensVendidos.setText(String.join("\n", reportService.resumoItensVendidos()));
            itemMaisVendido.setText(reportService.itemMaisVendido());
            itemMenosVendido.setText(reportService.itemMenosVendido());
        };
        this.caixaRefreshAction = refreshCaixa;
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
        fecharCaixa.setOnAction(e -> {
            if (AlertUtil.confirm(
                "Confirmar Fechamento de Caixa",
                "Fechar caixa",
                "Deseja realmente fechar o caixa? Esta acao vai zerar comandas e vendas."
            )) {
                comandaService.closeCaixa();
                refreshCaixa.run();
                refreshComandasInstant();
                refreshNumeroComandaPreview();
                AlertUtil.info("Caixa fechado e dados zerados.");
            }
        });
        Separator caixaSeparator = new Separator();
        VBox.setMargin(caixaSeparator, new Insets(14, 0, 0, 0));
        HBox pagamentoRow = labeledRow("Forma de Pagamento", pgto);
        pagamentoRow.getChildren().addAll(spacer(50), fecharCaixa);
        VBox leftPane = new VBox(8,
            labeledRow("Comanda para Fechamento", comanda),
            pagamentoRow,
            hbox(fechar),
            caixaSeparator,
            new Label("Lista de Comandas Fechadas (Valores)"),
            tabelaFechadas
        );
        GridPane cardsGrid = new GridPane();
        cardsGrid.setHgap(12);
        cardsGrid.setVgap(12);
        cardsGrid.add(createCard("Total vendido", 200, 70, totalVendido), 0, 0);
        cardsGrid.add(createCard("Comandas abertas", 200, 70, comandasAbertas), 1, 0);
        cardsGrid.add(createCard("Comandas fechadas", 200, 70, comandasFechadas), 2, 0);
        cardsGrid.add(createCard("Comanda maior valor", 200, 80, comandaMaior), 0, 1);
        cardsGrid.add(createCard("Comanda menor valor", 200, 80, comandaMenor), 1, 1);
        cardsGrid.add(createCard("Lucro liquido", 200, 80, lucroLiquido), 2, 1);
        cardsGrid.add(createCard("Forma de Pagamento", formasPagamentoScroll), 0, 2);
        cardsGrid.add(createCard("Itens vendidos", itensVendidosScroll), 1, 2);
        VBox itensExtremosBox = new VBox(12,
            createCard("Item mais vendido", 200, 70, itemMaisVendido),
            createCard("Item menos vendido", 200, 70, itemMenosVendido)
        );
        cardsGrid.add(itensExtremosBox, 2, 2);

        VBox rightPane = new VBox(10, cardsGrid);
        rightPane.setStyle("-fx-background-color: #1E1E1E;");
        ScrollPane rightScroll = new ScrollPane(rightPane);
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background: #1E1E1E; -fx-background-color: #1E1E1E;");
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

    private void refreshNumeroComandaPreview() {
        if (comandaNumeroPreviewField != null) comandaNumeroPreviewField.setText(comandaNumberService.peekNextNumber());
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

    private void bindColumnWidthPersistence(TableView<?> table, String keyPrefix) {
        for (TableColumn<?, ?> column : table.getColumns()) {
            String key = keyPrefix + "." + column.getText();
            double saved = persistedColumnWidths.getOrDefault(key, -1.0);
            if (saved > 0) column.setPrefWidth(saved);
            column.widthProperty().addListener((obs, oldV, newV) -> {
                persistedColumnWidths.put(key, newV.doubleValue());
                ConfigJsonStore.saveColumnWidths(persistedColumnWidths);
            });
        }
    }

    private VBox createCard(String title, javafx.scene.Node... content) {
        return createCard(title, 200, 200, content);
    }

    private VBox createCard(String title, double width, double height, javafx.scene.Node... content) {
        Label t = new Label(title);
        VBox card = new VBox(6);
        card.getChildren().add(t);
        card.getChildren().addAll(content);
        card.setPadding(new Insets(10));
        card.setPrefSize(width, height);
        card.setMinSize(width, height);
        card.setMaxSize(width, height);
        card.setStyle("-fx-background-color: #2A2A2A; -fx-border-color: #007BFF; -fx-border-width: 1.4; -fx-border-radius: 6; -fx-background-radius: 6;");
        return card;
    }
}
