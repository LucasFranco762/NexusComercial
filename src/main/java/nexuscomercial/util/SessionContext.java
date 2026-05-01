package nexuscomercial.util;

import nexuscomercial.model.User;

public final class SessionContext {
    private static User currentUser;

    private SessionContext() {}

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isAdmin() {
        return currentUser != null && "ADMINISTRADOR".equals(currentUser.getPerfil());
    }
}
