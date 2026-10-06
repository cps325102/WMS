package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PdaService {
    private final DataStorage storage;
    public PdaService(DataStorage storage) { this.storage = storage; }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> user = storage.findUserByUsername(username);
        if (user == null || !"123456".equals(password)) throw new RuntimeException("用户名或密码错误");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("token", "pda-token-" + UUID.randomUUID().toString().substring(0, 8));
        r.put("username", username);
        r.put("userId", user.get("id"));
        return r;
    }

    public Map<String, Object> scanKanban(String kanbanCode) {
        return storage.findKanbanByCode(kanbanCode);
    }

    public Map<String, Object> receive(String kanbanCode, BigDecimal qty, String operator, String batchNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kanbanCode", kanbanCode);
        body.put("receiveQty", qty);
        body.put("operator", operator);
        if (batchNo != null) body.put("batchNo", batchNo);
        // 复用已有的ReceiveService逻辑
        Map<String, Object> kanban = storage.findKanbanByCode(kanbanCode);
        if (kanban == null) throw new RuntimeException("看板不存在");
        BigDecimal planQty = toBigDecimal(kanban.get("planQty"));
        BigDecimal receivedQty = toBigDecimal(kanban.get("receivedQty"));
        BigDecimal remaining = planQty.subtract(receivedQty);
        if (qty.compareTo(remaining) > 0) throw new RuntimeException("入库数量超过待入库数量");

        BigDecimal newReceived = receivedQty.add(qty);
        kanban.put("receivedQty", newReceived);
        kanban.put("status", newReceived.compareTo(planQty) >= 0 ? "已完成" : "部分入库");
        storage.saveKanban(kanban);

        Long itemId = toLong(kanban.get("orderItemId"));
        if (itemId != null) {
            Map<String, Object> item = storage.findItemById(itemId);
            if (item != null) {
                BigDecimal itemReceived = toBigDecimal(item.get("receivedQty"));
                item.put("receivedQty", itemReceived.add(qty));
                item.put("status", itemReceived.add(qty).compareTo(toBigDecimal(item.get("planQty"))) >= 0 ? "已完成" : "部分入库");
                storage.saveItem(item);
            }
        }

        Long materialId = toLong(kanban.get("materialId"));
        Long warehouseId = toLong(kanban.get("warehouseId"));
        Long locationId = toLong(kanban.get("locationId"));
        String batch = batchNo != null ? batchNo : safeToString(kanban.get("batchNo"));
        Map<String, Object> inv = storage.findInventoryByKey(materialId, warehouseId, locationId, batch);
        if (inv == null) {
            inv = new LinkedHashMap<>();
            inv.put("materialId", materialId);
            inv.put("materialCode", kanban.get("materialCode"));
            inv.put("materialName", kanban.get("materialName"));
            inv.put("warehouseId", warehouseId);
            inv.put("warehouseName", kanban.get("warehouseName"));
            inv.put("locationId", locationId);
            inv.put("locationName", kanban.get("locationName"));
            inv.put("batchNo", batch);
            inv.put("qty", BigDecimal.ZERO);
        }
        BigDecimal beforeQty = toBigDecimal(inv.get("qty"));
        inv.put("qty", beforeQty.add(qty));
        storage.saveInventory(inv);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("recordNo", storage.generateNo("LS"));
        record.put("businessType", "PDA入库");
        record.put("businessNo", kanban.get("orderNo"));
        record.put("kanbanCode", kanbanCode);
        record.put("materialId", materialId);
        record.put("materialCode", kanban.get("materialCode"));
        record.put("materialName", kanban.get("materialName"));
        record.put("batchNo", batch);
        record.put("changeQty", qty);
        record.put("beforeQty", beforeQty);
        record.put("afterQty", beforeQty.add(qty));
        record.put("operator", operator);
        record.put("operateTime", now());
        record.put("remark", "PDA扫码入库");
        storage.saveRecord(record);
        return record;
    }

    public List<Map<String, Object>> getPickTasks(String deviceId) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> order : storage.findAllOutboundOrders()) {
            if (!"待出库".equals(order.get("status"))) continue;
            Long orderId = toLong(order.get("id"));
            for (Map<String, Object> item : storage.findOutboundItemsByOrderId(orderId)) {
                if (toBigDecimal(item.get("shippedQty")).compareTo(toBigDecimal(item.get("planQty"))) >= 0) continue;
                Long materialId = toLong(item.get("materialId"));
                List<Map<String, Object>> invs = storage.findAllInventories().stream()
                        .filter(inv -> Objects.equals(toLong(inv.get("materialId")), materialId))
                        .filter(inv -> toBigDecimal(inv.get("qty")).compareTo(BigDecimal.ZERO) > 0)
                        .sorted((a, b) -> safeToString(a.get("batchNo")).compareTo(safeToString(b.get("batchNo"))))
                        .collect(Collectors.toList());
                for (Map<String, Object> inv : invs) {
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("orderNo", order.get("orderNo"));
                    task.put("materialCode", item.get("materialCode"));
                    task.put("materialName", item.get("materialName"));
                    task.put("batchNo", inv.get("batchNo"));
                    task.put("locationName", inv.get("locationName"));
                    task.put("qty", inv.get("qty"));
                    task.put("planQty", item.get("planQty"));
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    public Map<String, Object> verifyLocation(String locationCode, String materialCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("locationCode", locationCode);
        result.put("materialCode", materialCode);
        for (Map<String, Object> inv : storage.findAllInventories()) {
            if (locationCode.equals(inv.get("locationName")) && materialCode.equals(inv.get("materialCode"))) {
                result.put("match", true);
                result.put("batchNo", inv.get("batchNo"));
                result.put("qty", inv.get("qty"));
                return result;
            }
        }
        result.put("match", false);
        return result;
    }

    public List<Map<String, Object>> queryInventory(String keyword) {
        List<Map<String, Object>> list = storage.findAllInventories();
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            list = list.stream().filter(inv -> safeToString(inv.get("materialCode")).toLowerCase().contains(lower)
                    || safeToString(inv.get("materialName")).toLowerCase().contains(lower)
                    || safeToString(inv.get("batchNo")).toLowerCase().contains(lower)
                    || safeToString(inv.get("locationName")).toLowerCase().contains(lower)).collect(Collectors.toList());
        }
        return list;
    }

    public Map<String, Object> submitCheck(Long inventoryId, BigDecimal checkQty, String operator, String remark) {
        Map<String, Object> inv = storage.findAllInventories().stream()
                .filter(i -> Objects.equals(i.get("id"), inventoryId)).findFirst().orElse(null);
        if (inv == null) throw new RuntimeException("库存记录不存在");
        BigDecimal oldQty = toBigDecimal(inv.get("qty"));
        inv.put("qty", checkQty);
        storage.saveInventory(inv);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("recordNo", storage.generateNo("LS"));
        record.put("businessType", "盘点");
        record.put("materialId", inv.get("materialId"));
        record.put("materialCode", inv.get("materialCode"));
        record.put("materialName", inv.get("materialName"));
        record.put("batchNo", inv.get("batchNo"));
        record.put("changeQty", checkQty.subtract(oldQty));
        record.put("beforeQty", oldQty);
        record.put("afterQty", checkQty);
        record.put("operator", operator);
        record.put("operateTime", now());
        record.put("remark", remark);
        storage.saveRecord(record);
        return record;
    }

    public List<Map<String, Object>> getCachedOps(String deviceId) {
        return storage.findAllPdaCaches().stream()
                .filter(c -> Objects.equals(c.get("deviceId"), deviceId))
                .filter(c -> !"synced".equals(c.get("syncStatus")))
                .collect(Collectors.toList());
    }

    public Map<String, Object> cacheOperation(String deviceId, String opType, Map<String, Object> data) {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("deviceId", deviceId);
        cache.put("opType", opType);
        cache.put("data", data);
        cache.put("syncStatus", "pending");
        return storage.savePdaCache(cache);
    }

    public List<Map<String, Object>> syncOfflineData(String deviceId) {
        List<Map<String, Object>> synced = new ArrayList<>();
        for (Map<String, Object> cache : storage.findAllPdaCaches()) {
            if (Objects.equals(cache.get("deviceId"), deviceId) && !"synced".equals(cache.get("syncStatus"))) {
                cache.put("syncStatus", "synced");
                storage.savePdaCache(cache);
                synced.add(cache);
            }
        }
        return synced;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try { return new BigDecimal(value.toString()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private String safeToString(Object obj) { return obj == null ? "" : obj.toString(); }
    private String now() { return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
}
