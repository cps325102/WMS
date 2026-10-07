package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryMonitorService {
    private final DataStorage storage;

    public InventoryMonitorService(DataStorage storage) {
        this.storage = storage;
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal frozen = BigDecimal.ZERO;
        int sku = 0;
        for (Map<String, Object> inv : storage.findAllInventories()) {
            total = total.add(toBigDecimal(inv.get("qty")));
            frozen = frozen.add(toBigDecimal(inv.get("frozenQty")));
            sku++;
        }
        overview.put("totalQty", total);
        overview.put("frozenQty", frozen);
        overview.put("availableQty", total.subtract(frozen).max(BigDecimal.ZERO));
        overview.put("skuCount", sku);
        overview.put("locationCount", storage.findAllLocations().size());
        overview.put("openAlertCount", storage.findAllAlertLogs().stream()
                .filter(a -> "待处理".equals(safeToString(a.get("status")))).count());
        return overview;
    }

    public List<Map<String, Object>> getLocationSummary() {
        Map<String, Map<String, Object>> summary = new LinkedHashMap<>();
        for (Map<String, Object> inv : storage.findAllInventories()) {
            String loc = safeToString(inv.get("locationName"));
            String mat = safeToString(inv.get("materialName"));
            String key = loc + "|" + mat;
            summary.putIfAbsent(key, new LinkedHashMap<>());
            Map<String, Object> s = summary.get(key);
            s.put("locationName", loc);
            s.put("materialName", mat);
            s.put("materialCode", inv.get("materialCode"));
            s.put("batchNo", inv.get("batchNo"));
            s.put("qty", toBigDecimal(s.getOrDefault("qty", BigDecimal.ZERO)).add(toBigDecimal(inv.get("qty"))));
            s.put("frozenQty", toBigDecimal(s.getOrDefault("frozenQty", BigDecimal.ZERO)).add(toBigDecimal(inv.get("frozenQty"))));
        }
        return new ArrayList<>(summary.values());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> checkAlerts() {
        List<Map<String, Object>> generated = new ArrayList<>();
        Map<Long, Map<String, Object>> configByMaterial = storage.findAllAlertConfigs().stream()
                .filter(c -> toLong(c.get("materialId")) != null)
                .collect(Collectors.toMap(c -> toLong(c.get("materialId")), c -> c, (a, b) -> a));
        Map<Long, BigDecimal> thirtyDayOutbound = calcThirtyDayOutbound();
        Map<Long, LocalDate> lastOutboundDate = calcLastOutboundDate();

        for (Map<String, Object> inv : storage.findAllInventories()) {
            BigDecimal qty = toBigDecimal(inv.get("qty"));
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
            Long materialId = toLong(inv.get("materialId"));
            if (materialId == null) continue;
            Map<String, Object> cfg = configByMaterial.getOrDefault(materialId, Map.of());
            if (!isEnabled(cfg)) continue;

            int leadTimeDays = valueOrDefault(cfg.get("leadTimeDays"), 7);
            int stagnantLimit = valueOrDefault(cfg.get("stagnantDays"), 180);
            BigDecimal dailyAvgOut = thirtyDayOutbound.getOrDefault(materialId, BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            BigDecimal threshold = dailyAvgOut.multiply(BigDecimal.valueOf(leadTimeDays));
            BigDecimal availableQty = qty.subtract(toBigDecimal(inv.get("frozenQty"))).max(BigDecimal.ZERO);

            if (threshold.compareTo(BigDecimal.ZERO) > 0 && availableQty.compareTo(threshold) < 0) {
                generated.add(upsertAlert("缺货预警", materialId, inv, qty, availableQty, threshold,
                        dailyAvgOut, leadTimeDays, stagnantLimit, "高",
                        "可用库存低于近30天日均出库量乘到货周期",
                        "建议尽快补货或调整出库计划"));
            }

            String batchNo = safeToString(inv.get("batchNo"));
            LocalDate lastOutbound = lastOutboundDate.get(materialId);
            LocalDate baseDate = lastOutbound != null ? lastOutbound : parseBatchDate(batchNo);
            long idleDays = baseDate == null ? stagnantLimit + 1L : ChronoUnit.DAYS.between(baseDate, LocalDate.now());
            if (idleDays >= stagnantLimit) {
                generated.add(upsertAlert("呆滞预警", materialId, inv, qty, availableQty, BigDecimal.ZERO,
                        dailyAvgOut, leadTimeDays, (int) idleDays, idleDays >= 270 ? "高" : "中",
                        "有库存物料超过" + stagnantLimit + "天未发生出库",
                        "建议复核需求、转库转包或做封存处理"));
            }
        }
        return generated;
    }

    public List<Map<String, Object>> listAlertLogs(String type, String status) {
        List<Map<String, Object>> logs = storage.findAllAlertLogs();
        if (type != null && !type.isBlank()) {
            logs = logs.stream().filter(l -> type.equals(l.get("alertType"))).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            logs = logs.stream().filter(l -> status.equals(l.get("status"))).collect(Collectors.toList());
        }
        return logs;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleAlert(Long id, String handler, String action, String remark) {
        Map<String, Object> log = storage.findAlertLogById(id);
        if (log == null) throw new RuntimeException("预警记录不存在");
        log.put("handler", handler);
        log.put("action", action);
        log.put("remark", remark);
        log.put("status", "已处理");
        log.put("handledAt", now());
        return storage.saveAlertLog(log);
    }

    public List<Map<String, Object>> listAlertConfigs() {
        List<Map<String, Object>> configs = storage.findAllAlertConfigs();
        for (Map<String, Object> cfg : configs) {
            Map<String, Object> material = storage.findMaterialById(toLong(cfg.get("materialId")));
            if (material != null) {
                cfg.put("materialCode", material.get("code"));
                cfg.put("materialName", material.get("name"));
            }
        }
        return configs;
    }

    public Map<String, Object> saveAlertConfig(Map<String, Object> body) {
        Map<String, Object> cfg = body.get("id") == null ? new LinkedHashMap<>() : storage.findAlertConfigById(toLong(body.get("id")));
        if (cfg == null) cfg = new LinkedHashMap<>();
        if (body.get("id") != null) cfg.put("id", body.get("id"));
        cfg.put("materialId", body.get("materialId"));
        cfg.put("leadTimeDays", body.getOrDefault("leadTimeDays", 7));
        cfg.put("stagnantDays", body.getOrDefault("stagnantDays", 180));
        cfg.put("enabled", body.getOrDefault("enabled", 1));
        return storage.saveAlertConfig(cfg);
    }

    public void deleteAlertConfig(Long id) {
        storage.deleteAlertConfig(id);
    }

    private Map<String, Object> upsertAlert(String type, Long materialId, Map<String, Object> inv,
                                            BigDecimal qty, BigDecimal availableQty, BigDecimal threshold,
                                            BigDecimal dailyAvgOut, int leadTimeDays, int stagnantDays,
                                            String level, String message, String suggestion) {
        String batchNo = safeToString(inv.get("batchNo"));
        Map<String, Object> alert = storage.findOpenAlert(type, materialId, batchNo);
        if (alert == null) {
            alert = new LinkedHashMap<>();
            alert.put("alertNo", storage.generateNo("YJ"));
            alert.put("status", "待处理");
        }
        alert.put("alertType", type);
        alert.put("level", level);
        alert.put("materialId", materialId);
        alert.put("materialCode", inv.get("materialCode"));
        alert.put("materialName", inv.get("materialName"));
        alert.put("warehouseName", inv.get("warehouseName"));
        alert.put("locationName", inv.get("locationName"));
        alert.put("batchNo", batchNo);
        alert.put("qty", qty);
        alert.put("availableQty", availableQty);
        alert.put("thresholdQty", threshold);
        alert.put("dailyAvgOut", dailyAvgOut);
        alert.put("leadTimeDays", leadTimeDays);
        alert.put("stagnantDays", stagnantDays);
        alert.put("message", message);
        alert.put("suggestion", suggestion);
        return storage.saveAlertLog(alert);
    }

    private Map<Long, BigDecimal> calcThirtyDayOutbound() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> record : storage.findAllRecords()) {
            if (!"出库".equals(safeToString(record.get("businessType")))) continue;
            LocalDateTime time = parseDateTime(record.get("operateTime"));
            if (time != null && time.isBefore(since)) continue;
            Long materialId = toLong(record.get("materialId"));
            BigDecimal qty = toBigDecimal(record.get("changeQty")).abs();
            if (materialId != null) result.merge(materialId, qty, BigDecimal::add);
        }
        return result;
    }

    private Map<Long, LocalDate> calcLastOutboundDate() {
        Map<Long, LocalDate> result = new HashMap<>();
        for (Map<String, Object> record : storage.findAllRecords()) {
            if (!"出库".equals(safeToString(record.get("businessType")))) continue;
            Long materialId = toLong(record.get("materialId"));
            LocalDateTime time = parseDateTime(record.get("operateTime"));
            if (materialId == null || time == null) continue;
            result.merge(materialId, time.toLocalDate(), (a, b) -> a.isAfter(b) ? a : b);
        }
        return result;
    }

    private LocalDate parseBatchDate(String batchNo) {
        try {
            String datePart = batchNo.replaceAll("\\D", "");
            if (datePart.length() >= 8) {
                return LocalDate.of(Integer.parseInt(datePart.substring(0, 4)),
                        Integer.parseInt(datePart.substring(4, 6)),
                        Integer.parseInt(datePart.substring(6, 8)));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        String text = value.toString();
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal v) return v;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int valueOrDefault(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean isEnabled(Map<String, Object> cfg) {
        Object value = cfg.get("enabled");
        if (value == null) return true;
        if (value instanceof Boolean b) return b;
        return !"0".equals(value.toString()) && !"false".equalsIgnoreCase(value.toString());
    }

    private String safeToString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
