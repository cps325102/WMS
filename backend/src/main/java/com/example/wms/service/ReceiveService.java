package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReceiveService {
    private final DataStorage storage;

    public ReceiveService(DataStorage storage) {
        this.storage = storage;
    }

    public Map<String, Object> receive(Map<String, Object> body) {
        String kanbanCode = (String) body.get("kanbanCode");
        BigDecimal receiveQty = new BigDecimal(body.get("receiveQty").toString());
        String operator = (String) body.getOrDefault("operator", "admin");

        if (receiveQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("入库数量必须大于0");
        }

        Map<String, Object> kanban = storage.findKanbanByCode(kanbanCode);
        if (kanban == null) throw new RuntimeException("看板不存在");

        BigDecimal planQty = new BigDecimal(kanban.get("planQty").toString());
        BigDecimal receivedQty = new BigDecimal(kanban.get("receivedQty").toString());
        BigDecimal remaining = planQty.subtract(receivedQty);
        if (receiveQty.compareTo(remaining) > 0) {
            throw new RuntimeException("本次入库数量超过待入库数量");
        }

        // 更新看板
        BigDecimal newReceived = receivedQty.add(receiveQty);
        kanban.put("receivedQty", newReceived);
        kanban.put("status", newReceived.compareTo(planQty) >= 0 ? "已完成" : "部分入库");
        storage.saveKanban(kanban);

        // 更新入库单明细
        Long itemId = (Long) kanban.get("orderItemId");
        Map<String, Object> item = storage.findItemById(itemId); // 需要增加此方法
        if (item != null) {
            BigDecimal itemReceived = new BigDecimal(item.get("receivedQty").toString());
            item.put("receivedQty", itemReceived.add(receiveQty));
            item.put("status", itemReceived.add(receiveQty).compareTo(new BigDecimal(item.get("planQty").toString())) >= 0 ? "已完成" : "部分入库");
            storage.saveItem(item);
            // 更新订单状态
            refreshOrderStatus((Long) item.get("orderId"));
        }

        // 更新库存
        Long materialId = (Long) kanban.get("materialId");
        Long warehouseId = (Long) kanban.get("warehouseId");
        Long locationId = (Long) kanban.get("locationId");
        String batchNo = (String) kanban.get("batchNo");
        Map<String, Object> inventory = storage.findInventoryByKey(materialId, warehouseId, locationId, batchNo);
        if (inventory == null) {
            inventory = new LinkedHashMap<>();
            inventory.put("materialId", materialId);
            inventory.put("materialCode", kanban.get("materialCode"));
            inventory.put("materialName", kanban.get("materialName"));
            inventory.put("warehouseId", warehouseId);
            inventory.put("warehouseName", kanban.get("warehouseName"));
            inventory.put("locationId", locationId);
            inventory.put("locationName", kanban.get("locationName"));
            inventory.put("batchNo", batchNo);
            inventory.put("qty", BigDecimal.ZERO);
        }
        BigDecimal beforeQty = new BigDecimal(inventory.get("qty").toString());
        BigDecimal afterQty = beforeQty.add(receiveQty);
        inventory.put("qty", afterQty);
        inventory.put("status", "已入库");
        storage.saveInventory(inventory);

        // 记录流水
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("recordNo", generateNo("LS"));
        record.put("businessType", "入库");
        record.put("businessNo", kanban.get("orderNo"));
        record.put("kanbanCode", kanbanCode);
        record.put("materialId", materialId);
        record.put("materialCode", kanban.get("materialCode"));
        record.put("materialName", kanban.get("materialName"));
        record.put("warehouseId", warehouseId);
        record.put("warehouseName", kanban.get("warehouseName"));
        record.put("locationId", locationId);
        record.put("locationName", kanban.get("locationName"));
        record.put("batchNo", batchNo);
        record.put("changeQty", receiveQty);
        record.put("beforeQty", beforeQty);
        record.put("afterQty", afterQty);
        record.put("operator", operator);
        record.put("operateTime", now());
        record.put("remark", "扫码入库");
        storage.saveRecord(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kanban", kanban);
        result.put("inventory", inventory);
        result.put("record", record);
        return result;
    }

    private void refreshOrderStatus(Long orderId) {
        List<Map<String, Object>> items = storage.findItemsByOrderId(orderId);
        boolean allDone = items.stream().allMatch(i ->
                new BigDecimal(i.get("receivedQty").toString()).compareTo(new BigDecimal(i.get("planQty").toString())) >= 0);
        boolean anyReceive = items.stream().anyMatch(i ->
                new BigDecimal(i.get("receivedQty").toString()).compareTo(BigDecimal.ZERO) > 0);
        Map<String, Object> order = storage.findOrderById(orderId);
        if (order != null) {
            order.put("status", allDone ? "已完成" : anyReceive ? "部分入库" : "待入库");
            storage.saveOrder(order);
        }
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + new Random().nextInt(1000);
    }
    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}