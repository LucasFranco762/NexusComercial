package nexuscomercial.service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;

public class ComandaNumberService {
    private static final int MAX = 999999;
    private final ConfigService configService = new ConfigService();

    public synchronized String nextNumber() {
        Map<String, String> cfg = configService.getAll();
        int seq = parseInt(cfg.getOrDefault("numeracao_seq_atual", "0"));
        String modo = cfg.getOrDefault("numeracao_modo", "FIM_DOS_NUMEROS");

        if ("FIM_DOS_NUMEROS".equals(modo) && seq >= MAX) seq = 0;
        seq++;
        configService.save(Map.of("numeracao_seq_atual", String.valueOf(seq)));
        return String.format("%06d", seq);
    }

    public synchronized String peekNextNumber() {
        Map<String, String> cfg = configService.getAll();
        int seq = parseInt(cfg.getOrDefault("numeracao_seq_atual", "0"));
        String modo = cfg.getOrDefault("numeracao_modo", "FIM_DOS_NUMEROS");
        if ("FIM_DOS_NUMEROS".equals(modo) && seq >= MAX) seq = 0;
        return String.format("%06d", seq + 1);
    }

    public void applyStartupResetPolicy() {
        Map<String, String> cfg = configService.getAll();
        String modo = cfg.getOrDefault("numeracao_modo", "FIM_DOS_NUMEROS");
        LocalDate hoje = LocalDate.now();
        LocalDate ultimoReset = parseDate(cfg.getOrDefault("numeracao_ultimo_reset", ""));
        LocalDate personalizada = parseDate(cfg.getOrDefault("numeracao_data_personalizada", ""));

        boolean reset = switch (modo) {
            case "DIARIO" -> ultimoReset == null || ultimoReset.isBefore(hoje);
            case "SEMANAL" -> mudouSemana(ultimoReset, hoje);
            case "MENSAL" -> ultimoReset == null || ultimoReset.getYear() != hoje.getYear() || ultimoReset.getMonthValue() != hoje.getMonthValue();
            case "ANUAL" -> ultimoReset == null || ultimoReset.getYear() != hoje.getYear();
            case "PERSONALIZADO" -> personalizada != null && !hoje.isBefore(personalizada) && (ultimoReset == null || ultimoReset.isBefore(personalizada));
            default -> false;
        };

        if (reset) {
            configService.save(Map.of(
                "numeracao_seq_atual", "0",
                "numeracao_ultimo_reset", hoje.toString()
            ));
        }
    }

    private boolean mudouSemana(LocalDate ultimo, LocalDate hoje) {
        if (ultimo == null) return true;
        WeekFields wf = WeekFields.of(Locale.getDefault());
        int w1 = ultimo.get(wf.weekOfWeekBasedYear());
        int w2 = hoje.get(wf.weekOfWeekBasedYear());
        int y1 = ultimo.get(wf.weekBasedYear());
        int y2 = hoje.get(wf.weekBasedYear());
        return w1 != w2 || y1 != y2;
    }

    private int parseInt(String v) {
        try { return Integer.parseInt(v); } catch (Exception e) { return 0; }
    }

    private LocalDate parseDate(String v) {
        try { return (v == null || v.isBlank()) ? null : LocalDate.parse(v); } catch (Exception e) { return null; }
    }
}
