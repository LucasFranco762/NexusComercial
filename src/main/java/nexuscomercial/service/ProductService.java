package nexuscomercial.service;

import nexuscomercial.dao.ProductDao;
import nexuscomercial.model.Product;

import java.util.List;
import java.util.stream.Collectors;

public class ProductService {
    private final ProductDao dao = new ProductDao();
    public List<Product> list() { try { return dao.findAll(); } catch (Exception e) { throw new RuntimeException(e); } }
    public List<Product> listActiveInStock() { try { return dao.findActiveInStock(); } catch (Exception e) { throw new RuntimeException(e); } }
    public void save(Product p) { try { if (p.getId() == 0) dao.save(p); else dao.update(p); } catch (Exception e) { throw new RuntimeException(e); } }
    public List<Product> lowStock() { return list().stream().filter(p -> p.getEstoque() <= p.getEstoqueMinimo()).collect(Collectors.toList()); }
}
