package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final DataStorage storage;
    public DashboardService(DataStorage storage) { this.storage = storage; }

    public Map<String, Object> getSummary() {
        Map<String, Object> s = new LinkedHashMap<>();
        long pi = storage.findAllOrders().stream().filter(o -> "待入库".equals(o.get("status"))).count();
        long po = storage.findAllOutboundOrders().stream().filter(o -> "待出库".equals(o.get("status"))).count();
        BigDecimal total = storage.findAllInventories().stream().map(i -> toBigDecimal(i.get("qty"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        s.put("pendingInbound", pi);
        s.put("pendingOutbound", po);
        s.put("totalInventory", total);
        s.put("pendingTasks", pi + po);
        return s;
    }

    public List<Map<String, Object>> getInboundTrend(int days) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        for (Map<String, Object> r : storage.findAllRecords()) {
            if (!"入库".equals(r.get("businessType")) && !"PDA入库".equals(r.get("businessType"))) continue;
            String time = safeToString(r.get("operateTime"));
            if (time.length() >= 10) {
                String day = time.substring(0, 10);
                trend.merge(day, toBigDecimal(r.get("changeQty")), BigDecimal::add);
            }
        }
        return trend.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", e.getKey()); m.put("qty", e.getValue()); return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getOutboundTrend(int days) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        for (Map<String, Object> r : storage.findAllRecords()) {
            if (!"出库".equals(r.get("businessType"))) continue;
            String time = safeToString(r.get("operateTime"));
            if (time.length() >= 10) {
                String day = time.substring(0, 10);
                trend.merge(day, toBigDecimal(r.get("changeQty")).abs(), BigDecimal::add);
            }
        }
        return trend.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", e.getKey()); m.put("qty", e.getValue()); return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getSlowMoving(int n) {
        Map<Long, Map<String, Object>> materialMap = new LinkedHashMap<>();
        for (Map<String, Object> inv : storage.findAllInventories()) {
            Long mid = toLong(inv.get("materialId"));
            if (mid == null) continue;
            materialMap.putIfAbsent(mid, new LinkedHashMap<>());
            Map<String, Object> m = materialMap.get(mid);
            m.put("materialCode", inv.get("materialCode"));
            m.put("materialName", inv.get("materialName"));
            m.put("qty", toBigDecimal(m.getOrDefault("qty", BigDecimal.ZERO)).add(toBigDecimal(inv.get("qty"))));
        }
        return materialMap.values().stream()
                .sorted((a, b) -> toBigDecimal(a.get("qty")).compareTo(toBigDecimal(b.get("qty"))))
                .limit(n > 0 ? n : 10)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getLocationHeatmap() {
        Map<String, Long> heat = new LinkedHashMap<>();
        for (Map<String, Object> inv : storage.findAllInventories()) {
            String loc = safeToString(inv.get("locationName"));
            heat.merge(loc, 1L, Long::sum);
        }
        return heat.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("location", e.getKey()); m.put("count", e.getValue()); return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> calcTurnover() {
        BigDecimal totalIn = storage.findAllRecords().stream()
                .filter(r -> "入库".equals(r.get("businessType")) || "PDA入库".equals(r.get("businessType")))
                .map(r -> toBigDecimal(r.get("changeQty"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = storage.findAllRecords().stream()
                .filter(r -> "出库".equals(r.get("businessType")))
                .map(r -> toBigDecimal(r.get("changeQty")).abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgInventory = storage.findAllInventories().stream()
                .map(i -> toBigDecimal(i.get("qty"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalInbound", totalIn);
        r.put("totalOutbound", totalOut);
        if (avgInventory.compareTo(BigDecimal.ZERO) > 0) {
            r.put("turnoverRate", totalOut.divide(avgInventory, 4, RoundingMode.HALF_UP));
        } else {
            r.put("turnoverRate", 0);
        }
        return r;
    }

    public Map<String, Object> calcCompletionRate() {
        List<Map<String, Object>> allOrders = storage.findAllOrders();
        long totalIn = allOrders.size();
        long doneIn = allOrders.stream().filter(o -> "已完成".equals(o.get("status"))).count();
        List<Map<String, Object>> allOut = storage.findAllOutboundOrders();
        long totalOut = allOut.size();
        long doneOut = allOut.stream().filter(o -> "已完成".equals(o.get("status"))).count();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("inboundCompletion", totalIn > 0 ? BigDecimal.valueOf(doneIn).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalIn), 1, RoundingMode.HALF_UP) : 0);
        r.put("outboundCompletion", totalOut > 0 ? BigDecimal.valueOf(doneOut).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalOut), 1, RoundingMode.HALF_UP) : 0);
        return r;
    }

    public List<Map<String, Object>> getPendingTasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> o : storage.findAllOrders()) {
            if ("待入库".equals(o.get("status"))) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "入库"); t.put("no", o.get("orderNo")); t.put("status", o.get("status"));
                tasks.add(t);
            }
        }
        for (Map<String, Object> o : storage.findAllOutboundOrders()) {
            if ("待出库".equals(o.get("status"))) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "出库"); t.put("no", o.get("orderNo")); t.put("status", o.get("status"));
                tasks.add(t);
            }
        }
        return tasks;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try { return new BigDecimal(value.toString()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }
    private String safeToString(Object obj) { return obj == null ? "" : obj.toString(); }
}
