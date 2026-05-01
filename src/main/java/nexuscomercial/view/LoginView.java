package nexuscomercial.view;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import nexuscomercial.service.AuthService;
import nexuscomercial.util.AlertUtil;

public class LoginView {
    private final Stage stage;
    private final AuthService authService = new AuthService();

    public LoginView(Stage stage) { this.stage = stage; }

    public Parent build() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(24));
        grid.setHgap(12);
        grid.setVgap(12);

        Label title = new Label("NexusComercial");
        title.getStyleClass().add("title");
        TextField user = new TextField("admin");
        PasswordField pass = new PasswordField();
        pass.setText("admin");
        Button btn = new Button("Entrar");

        grid.add(title, 0, 0, 2, 1);
        grid.add(new Label("Usuário"), 0, 1); grid.add(user, 1, 1);
        grid.add(new Label("Senha"), 0, 2); grid.add(pass, 1, 2);
        grid.add(btn, 1, 3);

        btn.setOnAction(e -> {
            if (authService.login(user.getText().trim(), pass.getText().trim())) {
                MainView mainView = new MainView(stage);
                Scene scene = new Scene(mainView.build(), 1300, 760);
                scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("NexusComercial");
                stage.setMaximized(true);
            } else {
                AlertUtil.error("Usuário ou senha inválidos.");
            }
        });
        return grid;
    }
}
