package nexuscomercial.util;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public final class AlertUtil {
    private AlertUtil() {}

    public static void info(String msg) { show(Alert.AlertType.INFORMATION, msg); }
    public static boolean confirm(String title, String header, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(msg);
        style(a);
        return a.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK;
    }
    public static void error(String msg) { show(Alert.AlertType.ERROR, msg); }
    private static void show(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        style(a);
        a.showAndWait();
    }

    private static void style(Alert a) {
        DialogPane pane = a.getDialogPane();
        pane.getStylesheets().add(AlertUtil.class.getResource("/css/theme.css").toExternalForm());
        pane.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #007BFF;");
    }
}
