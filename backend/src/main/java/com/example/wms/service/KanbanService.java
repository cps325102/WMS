package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KanbanService {
    private final DataStorage storage;

    public KanbanService(DataStorage storage) {
        this.storage = storage;
    }

    public List<Map<String, Object>> generateKanban(Long orderId) {
        Map<String, Object> order = storage.findOrderById(orderId);
        if (order == null) throw new RuntimeException("入库单不存在");
        List<Map<String, Object>> created = new ArrayList<>();
        List<Map<String, Object>> items = storage.findItemsByOrderId(orderId);
        for (Map<String, Object> item : items) {
            Long itemId = (Long) item.get("id");
            boolean exists = storage.findAllKanbans().stream()
                    .anyMatch(k -> Objects.equals(k.get("orderItemId"), itemId));
            if (exists) continue;

            // 获取物料的包装容量
            Long materialId = toLong(item.get("materialId"));
            Map<String, Object> material = materialId == null ? null : storage.findMaterialById(materialId);
            Integer packageQty = material == null ? null : toInteger(material.get("packageQty"));

            BigDecimal planQty = toBigDecimal(item.get("planQty"));

            // 如果有包装容量且大于0，按包装拆分
            if (packageQty != null && packageQty > 0) {
                int totalPackages = planQty.intValue() / packageQty;
                int remainder = planQty.intValue() % packageQty;

                // 生成完整包装的看板
                for (int i = 0; i < totalPackages; i++) {
                    Map<String, Object> kanban = createKanbanForPackage(
                        orderId, order, itemId, item,
                        new BigDecimal(packageQty),
                        i + 1, totalPackages + (remainder > 0 ? 1 : 0)
                    );
                    storage.saveKanban(kanban);
                    created.add(kanban);
                }

                // 如果有剩余数量，生成零头看板
                if (remainder > 0) {
                    Map<String, Object> kanban = createKanbanForPackage(
                        orderId, order, itemId, item,
                        new BigDecimal(remainder),
                        totalPackages + 1, totalPackages + 1
                    );
                    storage.saveKanban(kanban);
                    created.add(kanban);
                }
            } else {
                // 没有包装容量，生成一个看板
                Map<String, Object> kanban = createKanbanForPackage(
                    orderId, order, itemId, item,
                    planQty, 1, 1
                );
                storage.saveKanban(kanban);
                created.add(kanban);
            }
        }
        return created;
    }

    private Map<String, Object> createKanbanForPackage(
        Long orderId, Map<String, Object> order, Long orderItemId,
        Map<String, Object> item, BigDecimal qty,
        int currentPackage, int totalPackages) {

        Map<String, Object> kanban = new LinkedHashMap<>();
        String packageSuffix = totalPackages > 1 ? "-" + currentPackage : "";
        kanban.put("kanbanCode", generateCode() + packageSuffix);
        kanban.put("orderId", orderId);
        kanban.put("orderNo", order.get("orderNo"));
        kanban.put("orderItemId", orderItemId);
        kanban.put("materialId", item.get("materialId"));
        kanban.put("materialCode", item.get("materialCode"));
        kanban.put("materialName", item.get("materialName"));
        kanban.put("planQty", qty);
        kanban.put("receivedQty", BigDecimal.ZERO);
        kanban.put("warehouseId", item.get("warehouseId"));
        kanban.put("warehouseName", item.get("warehouseName"));
        kanban.put("locationId", item.get("locationId"));
        kanban.put("locationName", item.get("locationName"));
        kanban.put("batchNo", item.get("batchNo"));
        kanban.put("status", "未打印");
        kanban.put("printCount", 0);
        kanban.put("packageInfo", currentPackage + "/" + totalPackages);
        return kanban;
    }

    // ... existing code ...

    public List<Map<String, Object>> listKanbans(String keyword, String status) {
        List<Map<String, Object>> list = storage.findAllKanbans();
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            list = list.stream().filter(k ->
                    contains(k.get("kanbanCode"), lower) ||
                    contains(k.get("orderNo"), lower) ||
                    contains(k.get("materialName"), lower) ||
                    contains(k.get("materialCode"), lower)
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(k -> status.equals(k.get("status"))).collect(Collectors.toList());
        }
        list.sort((a, b) -> {
            String ta = (String) a.getOrDefault("createTime", "");
            String tb = (String) b.getOrDefault("createTime", "");
            return tb.compareTo(ta);
        });
        return list;
    }

    private boolean contains(Object val, String keyword) {
        return val != null && val.toString().toLowerCase().contains(keyword);
    }

    public Map<String, Object> printKanban(Long kanbanId) {
        Map<String, Object> kanban = storage.findKanbanById(kanbanId);
        if (kanban == null) throw new RuntimeException("看板不存在");
        int count = (int) kanban.getOrDefault("printCount", 0);
        kanban.put("printCount", count + 1);
        kanban.put("lastPrintTime", now());
        if ("未打印".equals(kanban.get("status"))) {
            kanban.put("status", "已打印");
        }
        return storage.saveKanban(kanban);
    }

    public Map<String, Object> scanKanban(String kanbanCode) {
        Map<String, Object> kanban = storage.findKanbanByCode(kanbanCode);
        if (kanban == null) throw new RuntimeException("看板不存在");
        return kanban;
    }

    public Map<String, Object> kanbanTrace(String kanbanCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kanban", scanKanban(kanbanCode));
        List<Map<String, Object>> records = storage.findAllRecords().stream()
                .filter(r -> Objects.equals(r.get("kanbanCode"), kanbanCode))
                .collect(Collectors.toList());
        result.put("records", records);
        return result;
    }

    private String generateCode() {
        return "KB" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
    private String now() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 辅助方法：转换为 Integer
    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ... existing code ...

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
