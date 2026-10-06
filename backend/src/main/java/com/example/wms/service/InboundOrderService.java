package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InboundOrderService {
    private final DataStorage storage;

    public InboundOrderService(DataStorage storage) {
        this.storage = storage;
    }

    public Map<String, Object> createOrder(Map<String, Object> body) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderNo", generateNo("RK"));
        order.put("inboundType", body.getOrDefault("inboundType", "采购入库"));

        // 安全转换 supplierId
        Long supplierId = toLong(body.get("supplierId"));
        order.put("supplierId", supplierId);
        Map<String, Object> supplier = supplierId == null ? null : storage.findSupplierById(supplierId);
        order.put("supplierName", supplier == null ? "" : supplier.get("supplierName"));

        order.put("planDate", body.get("planDate"));
        order.put("status", "待入库");
        order.put("remark", body.getOrDefault("remark", ""));
        order.put("createBy", body.getOrDefault("createBy", "admin"));
        Map<String, Object> saved = storage.saveOrder(order);
        Long orderId = toLong(saved.get("id"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items != null) {
            for (Map<String, Object> itemBody : items) {
                createOrderItem(orderId, itemBody);
            }
        }
        return detailOrder(orderId);
    }

    private void createOrderItem(Long orderId, Map<String, Object> body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("orderId", orderId);

        Long materialId = toLong(body.get("materialId"));
        Map<String, Object> material = materialId == null ? null : storage.findMaterialById(materialId);
        item.put("materialId", materialId);
        item.put("materialCode", material == null ? "" : safeGet(material, "code"));
        item.put("materialName", material == null ? "" : safeGet(material, "name"));
        item.put("spec", material == null ? "" : safeGet(material, "spec"));

        Object qtyObj = body.get("planQty");
        item.put("planQty", qtyObj == null ? BigDecimal.ZERO : new BigDecimal(qtyObj.toString()));
        item.put("receivedQty", BigDecimal.ZERO);
        item.put("unit", body.getOrDefault("unit", "件"));

        Long warehouseId = toLong(body.get("warehouseId"));
        Map<String, Object> warehouse = warehouseId == null ? null : storage.findWarehouseById(warehouseId);
        item.put("warehouseId", warehouseId);
        item.put("warehouseName", warehouse == null ? "" : safeGet(warehouse, "name"));

        Long locationId = toLong(body.get("locationId"));
        Map<String, Object> location = locationId == null ? null : storage.findLocationById(locationId);
        item.put("locationId", locationId);
        item.put("locationName", location == null ? "" : safeGet(location, "name"));

        item.put("batchNo", body.getOrDefault("batchNo", generateNo("PC")));
        item.put("status", "待入库");
        storage.saveItem(item);
    }

    private String safeGet(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    public Map<String, Object> updateOrder(Long orderId, Map<String, Object> body) {
        Map<String, Object> order = storage.findOrderById(orderId);
        if (order == null) throw new RuntimeException("入库单不存在");
        if (!List.of("草稿", "待入库").contains(order.get("status"))) {
            throw new RuntimeException("当前状态不允许修改");
        }
        order.put("inboundType", body.getOrDefault("inboundType", order.get("inboundType")));
        Long supplierId = toLong(body.getOrDefault("supplierId", order.get("supplierId")));
        order.put("supplierId", supplierId);
        Map<String, Object> supplier = supplierId == null ? null : storage.findSupplierById(supplierId);
        order.put("supplierName", supplier == null ? "" : supplier.get("supplierName"));
        order.put("planDate", body.getOrDefault("planDate", order.get("planDate")));
        order.put("remark", body.getOrDefault("remark", order.get("remark")));
        storage.saveOrder(order);

        storage.deleteItemsByOrderId(orderId);
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items != null) {
            for (Map<String, Object> itemBody : items) {
                createOrderItem(orderId, itemBody);
            }
        }
        storage.deleteKanbansByOrderId(orderId);
        return detailOrder(orderId);
    }

    public List<Map<String, Object>> listOrders(String keyword, String status) {
        List<Map<String, Object>> orders = storage.findAllOrders();
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            orders = orders.stream().filter(o ->
                    o.get("orderNo").toString().toLowerCase().contains(lower) ||
                            o.get("supplierName").toString().toLowerCase().contains(lower)
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            orders = orders.stream().filter(o -> status.equals(o.get("status"))).collect(Collectors.toList());
        }
        // 计算每个订单的总计划数量和已入库数量
        for (Map<String, Object> order : orders) {
            Long orderId = (Long) order.get("id");
            List<Map<String, Object>> items = storage.findItemsByOrderId(orderId);
            BigDecimal totalPlan = items.stream().map(i -> new BigDecimal(i.get("planQty").toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalReceived = items.stream().map(i -> new BigDecimal(i.get("receivedQty").toString())).reduce(BigDecimal.ZERO, BigDecimal::add);
            order.put("totalPlanQty", totalPlan);
            order.put("totalReceivedQty", totalReceived);
            order.put("itemCount", items.size());
        }
        return orders;
    }

    public Map<String, Object> detailOrder(Long orderId) {
        Map<String, Object> order = storage.findOrderById(orderId);
        if (order == null) throw new RuntimeException("入库单不存在");
        order.put("items", storage.findItemsByOrderId(orderId));
        return order;
    }

    public void deleteOrder(Long orderId) {
        Map<String, Object> order = storage.findOrderById(orderId);
        if (order == null) return;
        if (!List.of("草稿", "待入库").contains(order.get("status"))) {
            throw new RuntimeException("当前状态不允许删除");
        }
        storage.deleteOrder(orderId);
        storage.deleteItemsByOrderId(orderId);
        storage.deleteKanbansByOrderId(orderId);
    }

    // 安全转换为 Long 的辅助方法
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 反审核（可追溯、可反审核） ====================
    public Map<String, Object> reverseAudit(Long orderId, String reason, String operator) {
        Map<String, Object> order = storage.findOrderById(orderId);
        if (order == null) throw new RuntimeException("入库单不存在");
        if (!"已完成".equals(order.get("status"))) {
            throw new RuntimeException("只有已完成状态的单据可反审核");
        }
        List<Map<String, Object>> items = storage.findItemsByOrderId(orderId);
        for (Map<String, Object> item : items) {
            BigDecimal receivedQty = toBigDecimal(item.get("receivedQty"));
            if (receivedQty.compareTo(BigDecimal.ZERO) <= 0) continue;
            Long materialId = toLong(item.get("materialId"));
            Long warehouseId = toLong(item.get("warehouseId"));
            Long locationId = toLong(item.get("locationId"));
            String batchNo = safeToString(item.get("batchNo"));
            Map<String, Object> inventory = storage.findInventoryByKey(materialId, warehouseId, locationId, batchNo);
            if (inventory == null) continue;
            BigDecimal currentQty = toBigDecimal(inventory.get("qty"));
            BigDecimal newQty = currentQty.subtract(receivedQty).max(BigDecimal.ZERO);
            inventory.put("qty", newQty);
            storage.saveInventory(inventory);
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("recordNo", generateNo("LS"));
            record.put("businessType", "入库反审");
            record.put("businessNo", order.get("orderNo"));
            record.put("materialId", materialId);
            record.put("materialCode", item.get("materialCode"));
            record.put("materialName", item.get("materialName"));
            record.put("batchNo", batchNo);
            record.put("changeQty", receivedQty.negate());
            record.put("beforeQty", currentQty);
            record.put("afterQty", newQty);
            record.put("operator", operator);
            record.put("operateTime", now());
            record.put("remark", "反审核-" + reason);
            storage.saveRecord(record);
            item.put("receivedQty", BigDecimal.ZERO);
            item.put("status", "待入库");
            storage.saveItem(item);
        }
        order.put("status", "已撤销");
        order.put("reverseReason", reason);
        storage.saveOrder(order);
        storage.deleteKanbansByOrderId(orderId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "反审核成功");
        result.put("orderNo", order.get("orderNo"));
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try { return new BigDecimal(value.toString()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private String safeToString(Object obj) { return obj == null ? "" : obj.toString(); }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + new Random().nextInt(1000);
    }
}