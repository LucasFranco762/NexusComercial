package nexuscomercial.service;

import nexuscomercial.dao.UserDao;
import nexuscomercial.model.User;

import java.util.List;

public class UserService {
    private final UserDao dao = new UserDao();

    public List<User> list() { try { return dao.findAll(); } catch (Exception e) { throw new RuntimeException(e); } }
    public void save(User u) { try { if (u.getId() == 0) dao.save(u); else dao.update(u); } catch (Exception e) { throw new RuntimeException(e); } }
}
