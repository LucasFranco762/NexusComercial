package nexuscomercial.service;

import nexuscomercial.dao.UserDao;
import nexuscomercial.model.User;
import nexuscomercial.util.SessionContext;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public boolean login(String usuario, String senha) {
        try {
            User u = userDao.findByCredentials(usuario, senha);
            SessionContext.setCurrentUser(u);
            return u != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
