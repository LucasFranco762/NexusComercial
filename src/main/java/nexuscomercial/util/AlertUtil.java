package nexuscomercial.util;

import javafx.scene.control.Alert;

public final class AlertUtil {
    private AlertUtil() {}

    public static void info(String msg) { show(Alert.AlertType.INFORMATION, msg); }
    public static void error(String msg) { show(Alert.AlertType.ERROR, msg); }
    private static void show(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
