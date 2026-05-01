package nexuscomercial.service;

import nexuscomercial.dao.CategoryDao;
import nexuscomercial.model.Category;

import java.util.List;

public class CategoryService {
    private final CategoryDao dao = new CategoryDao();
    public List<Category> list() { try { return dao.findAll(); } catch (Exception e) { throw new RuntimeException(e); } }
    public void save(String nome) { try { dao.save(nome); } catch (Exception e) { throw new RuntimeException(e); } }
    public void update(int id, String nome) { try { dao.update(id, nome); } catch (Exception e) { throw new RuntimeException(e); } }
    public void delete(int id) { try { dao.delete(id); } catch (Exception e) { throw new RuntimeException(e); } }
}
