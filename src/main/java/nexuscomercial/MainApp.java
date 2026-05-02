package nexuscomercial;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nexuscomercial.service.ComandaNumberService;
import nexuscomercial.util.DatabaseManager;
import nexuscomercial.view.LoginView;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        DatabaseManager.initializeDatabase();
        new ComandaNumberService().applyStartupResetPolicy();
        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView.build(), 520, 320);
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("NexusComercial - Login");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
