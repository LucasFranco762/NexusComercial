package nexuscomercial.service;

import nexuscomercial.dao.ConfigDao;

import java.util.Map;

public class ConfigService {
    private final ConfigDao dao = new ConfigDao();
    public Map<String, String> getAll() { try { return dao.getAll(); } catch (Exception e) { throw new RuntimeException(e); } }
    public double defaultLimit() { return Double.parseDouble(getAll().getOrDefault("limite_padrao", "200")); }
    public void save(Map<String, String> cfg) {
        try { for (var e : cfg.entrySet()) dao.save(e.getKey(), e.getValue()); } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
