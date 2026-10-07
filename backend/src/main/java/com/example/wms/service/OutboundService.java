package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutboundService {
    private static final Logger log = LoggerFactory.getLogger(OutboundService.class);
    private final DataStorage storage;

    public OutboundService(DataStorage storage) {
        this.storage = storage;
    }

    // ==================== 出库单 CRUD ====================

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOutboundOrder(Map<String, Object> body) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderNo", generateNo("CK"));
        order.put("outboundType", body.getOrDefault("outboundType", "销售出库"));
        order.put("customerId", toLong(body.get("customerId")));
        order.put("customerName", body.getOrDefault("customerName", ""));
        order.put("planDate", body.get("planDate"));
        order.put("status", "待出库");
        order.put("remark", body.getOrDefault("remark", ""));
        order.put("createBy", body.getOrDefault("createBy", "admin"));
        Map<String, Object> saved = storage.saveOutboundOrder(order);
        Long orderId = toLong(saved.get("id"));

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items != null) {
            for (Map<String, Object> itemBody : items) {
                createOutboundItem(orderId, itemBody);
            }
        }
        return detailOutboundOrder(orderId);
    }

    private void createOutboundItem(Long orderId, Map<String, Object> body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("orderId", orderId);
        Long materialId = toLong(body.get("materialId"));
        Map<String, Object> material = materialId == null ? null : storage.findMaterialById(materialId);
        item.put("materialId", materialId);
        item.put("materialCode", material == null ? "" : safeToString(material.get("code")));
        item.put("materialName", material == null ? "" : safeToString(material.get("name")));
        item.put("spec", material == null ? "" : safeToString(material.get("spec")));
        item.put("planQty", toBigDecimal(body.get("planQty")));
        item.put("shippedQty", BigDecimal.ZERO);
        item.put("unit", body.getOrDefault("unit", "件"));
        item.put("status", "待出库");
        storage.saveOutboundItem(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateOutboundOrder(Long orderId, Map<String, Object> body) {
        Map<String, Object> order = storage.findOutboundOrderById(orderId);
        if (order == null) throw new RuntimeException("出库单不存在");
        if (!List.of("草稿", "待出库").contains(order.get("status"))) {
            throw new RuntimeException("当前状态不允许修改");
        }
        order.put("outboundType", body.getOrDefault("outboundType", order.get("outboundType")));
        order.put("customerName", body.getOrDefault("customerName", order.get("customerName")));
        order.put("planDate", body.getOrDefault("planDate", order.get("planDate")));
        order.put("remark", body.getOrDefault("remark", order.get("remark")));
        storage.saveOutboundOrder(order);

        storage.deleteOutboundItemsByOrderId(orderId);
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items != null) {
            for (Map<String, Object> itemBody : items) {
                createOutboundItem(orderId, itemBody);
            }
        }
        return detailOutboundOrder(orderId);
    }

    public List<Map<String, Object>> listOutboundOrders(String keyword, String status) {
        List<Map<String, Object>> orders = storage.findAllOutboundOrders();
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            orders = orders.stream().filter(o ->
                    safeToString(o.get("orderNo")).toLowerCase().contains(lower) ||
                            safeToString(o.get("customerName")).toLowerCase().contains(lower)
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            orders = orders.stream().filter(o -> status.equals(safeToString(o.get("status")))).collect(Collectors.toList());
        }
        for (Map<String, Object> order : orders) {
            Long orderId = (Long) order.get("id");
            List<Map<String, Object>> items = storage.findOutboundItemsByOrderId(orderId);
            BigDecimal totalPlan = items.stream().map(i -> toBigDecimal(i.get("planQty"))).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalShipped = items.stream().map(i -> toBigDecimal(i.get("shippedQty"))).reduce(BigDecimal.ZERO, BigDecimal::add);
            order.put("totalPlanQty", totalPlan);
            order.put("totalShippedQty", totalShipped);
            order.put("itemCount", items.size());
        }
        return orders;
    }

    public Map<String, Object> detailOutboundOrder(Long orderId) {
        Map<String, Object> order = storage.findOutboundOrderById(orderId);
        if (order == null) throw new RuntimeException("出库单不存在");
        order.put("items", storage.findOutboundItemsByOrderId(orderId));
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOutboundOrder(Long orderId) {
        Map<String, Object> order = storage.findOutboundOrderById(orderId);
        if (order == null) return;
        if (!List.of("草稿", "待出库").contains(order.get("status"))) {
            throw new RuntimeException("当前状态不允许删除");
        }
        storage.deleteOutboundOrder(orderId);
        storage.deleteOutboundItemsByOrderId(orderId);
    }

    // ==================== 执行出库（先进先出） ====================

    /**
     * 先进先出执行出库：扣减多条库存、更新明细、写流水、回写看板状态，
     * 中间任何一步抛错都必须整体回滚，否则会扣了库存却没有出库记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeOutbound(Long orderId) {
        Map<String, Object> order = storage.findOutboundOrderById(orderId);
        if (order == null) throw new RuntimeException("出库单不存在");
        if (!"待出库".equals(order.get("status"))) {
            throw new RuntimeException("只有待出库状态的单据可执行");
        }

        List<Map<String, Object>> items = storage.findOutboundItemsByOrderId(orderId);
        if (items.isEmpty()) throw new RuntimeException("出库单无明细");

        List<Map<String, Object>> allocationDetails = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Long materialId = toLong(item.get("materialId"));
            if (materialId == null) continue;
            BigDecimal planQty = toBigDecimal(item.get("planQty"));
            BigDecimal shippedAlready = toBigDecimal(item.get("shippedQty"));
            BigDecimal needQty = planQty.subtract(shippedAlready);
            if (needQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 扣减前总库存
            BigDecimal beforeTotal = getTotalInventoryForMaterial(materialId);

            // 获取该物料所有有库存的记录，按批次升序
            List<Map<String, Object>> inventories = storage.findAllInventories().stream()
                    .filter(inv -> Objects.equals(toLong(inv.get("materialId")), materialId))
                    .filter(inv -> availableQty(inv).compareTo(BigDecimal.ZERO) > 0)
                    .sorted((a, b) -> {
                        String batchA = safeToString(a.get("batchNo"));
                        String batchB = safeToString(b.get("batchNo"));
                        if (batchA.isEmpty()) return 1;
                        if (batchB.isEmpty()) return -1;
                        return batchA.compareTo(batchB);
                    })
                    .collect(Collectors.toList());

            if (inventories.isEmpty()) {
                throw new RuntimeException("物料 " + materialId + " 库存不足，无法出库");
            }

            BigDecimal remaining = needQty;
            List<Map<String, Object>> usedInventories = new ArrayList<>();
            for (Map<String, Object> inv : inventories) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal currentQty = toBigDecimal(inv.get("qty"));
                BigDecimal deduct = remaining.min(availableQty(inv));
                BigDecimal newQty = currentQty.subtract(deduct);
                inv.put("qty", newQty);
                storage.saveInventory(inv);
                remaining = remaining.subtract(deduct);
                usedInventories.add(inv);

                Map<String, Object> alloc = new LinkedHashMap<>();
                alloc.put("materialCode", item.get("materialCode"));
                alloc.put("materialName", item.get("materialName"));
                alloc.put("batchNo", safeToString(inv.get("batchNo")));
                alloc.put("warehouseName", safeToString(inv.get("warehouseName")));
                alloc.put("locationName", safeToString(inv.get("locationName")));
                alloc.put("deductQty", deduct);
                allocationDetails.add(alloc);
            }

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("物料 " + materialId + " 库存不足，实际扣减量不足");
            }

            // 扣减后总库存
            BigDecimal afterTotal = getTotalInventoryForMaterial(materialId);

            // 更新出库单明细
            BigDecimal newShipped = shippedAlready.add(needQty);
            item.put("shippedQty", newShipped);
            item.put("status", newShipped.compareTo(planQty) >= 0 ? "已完成" : "部分出库");
            storage.saveOutboundItem(item);

            // 记录流水
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("recordNo", generateNo("LS"));
            record.put("businessType", "出库");
            record.put("businessNo", order.get("orderNo"));
            record.put("materialId", materialId);
            record.put("materialCode", item.get("materialCode"));
            record.put("materialName", item.get("materialName"));
            record.put("changeQty", needQty.negate());
            record.put("beforeQty", beforeTotal);
            record.put("afterQty", afterTotal);
            record.put("operator", "system");
            record.put("operateTime", now());
            record.put("remark", "先进先出自动出库");
            storage.saveRecord(record);

            // 更新关联的入库看板状态
            updateKanbanStatusForMaterial(materialId, usedInventories);
        }

        refreshOutboundOrderStatus(orderId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "出库成功");
        result.put("orderNo", order.get("orderNo"));
        result.put("allocationDetails", allocationDetails);
        return result;
    }

    private void refreshOutboundOrderStatus(Long orderId) {
        List<Map<String, Object>> items = storage.findOutboundItemsByOrderId(orderId);
        boolean allDone = items.stream().allMatch(i ->
                toBigDecimal(i.get("shippedQty")).compareTo(toBigDecimal(i.get("planQty"))) >= 0);
        boolean anyShip = items.stream().anyMatch(i ->
                toBigDecimal(i.get("shippedQty")).compareTo(BigDecimal.ZERO) > 0);
        Map<String, Object> order = storage.findOutboundOrderById(orderId);
        if (order != null) {
            order.put("status", allDone ? "已完成" : anyShip ? "部分出库" : "待出库");
            storage.saveOutboundOrder(order);
        }
    }

    private void updateKanbanStatusForMaterial(Long materialId, List<Map<String, Object>> usedInventories) {
        List<Map<String, Object>> kanbans = storage.findAllKanbans().stream()
                .filter(k -> Objects.equals(toLong(k.get("materialId")), materialId))
                .filter(k -> "已完成".equals(k.get("status")))
                .sorted((a, b) -> {
                    String batchA = safeToString(a.get("batchNo"));
                    String batchB = safeToString(b.get("batchNo"));
                    if (batchA.isEmpty()) return 1;
                    if (batchB.isEmpty()) return -1;
                    return batchA.compareTo(batchB);
                })
                .collect(Collectors.toList());

        for (Map<String, Object> inv : usedInventories) {
            String batchNo = safeToString(inv.get("batchNo"));
            if (batchNo.isEmpty()) continue;
            kanbans.stream()
                    .filter(k -> batchNo.equals(k.get("batchNo")))
                    .findFirst()
                    .ifPresent(k -> {
                        k.put("status", "已出库");
                        storage.saveKanban(k);
                    });
        }
    }

    // ==================== 库存查询辅助 ====================

    public List<Map<String, Object>> getAvailableInventoryForMaterial(Long materialId) {
        return storage.findAllInventories().stream()
                .filter(inv -> Objects.equals(toLong(inv.get("materialId")), materialId))
                .filter(inv -> availableQty(inv).compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> {
                    String batchA = safeToString(a.get("batchNo"));
                    String batchB = safeToString(b.get("batchNo"));
                    if (batchA.isEmpty()) return 1;
                    if (batchB.isEmpty()) return -1;
                    return batchA.compareTo(batchB);
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMaterialsWithStock() {
        // 汇总每个物料的库存总量
        Map<Long, BigDecimal> stockMap = new LinkedHashMap<>();
        for (Map<String, Object> inv : storage.findAllInventories()) {
            Long mid = toLong(inv.get("materialId"));
            BigDecimal qty = toBigDecimal(inv.get("qty"));
            if (mid != null) {
                stockMap.merge(mid, qty, BigDecimal::add);
            }
        }

        // 返回所有物料，带库存数量
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> mat : storage.findAllMaterials()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", mat.get("id"));
            m.put("materialCode", safeToString(mat.get("code")));
            m.put("materialName", safeToString(mat.get("name")));
            m.put("spec", safeToString(mat.get("spec")));
            m.put("unit", safeToString(mat.get("unit")));
            m.put("qty", stockMap.getOrDefault(mat.get("id"), BigDecimal.ZERO));
            result.add(m);
        }
        return result;
    }


    // ==================== 反审核 ====================
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reverseAudit(Long orderId, String reason, String operator) {
        Map<String, Object> order = storage.findOutboundOrderById(orderId);
        if (order == null) throw new RuntimeException("出库单不存在");
        if (!"已完成".equals(order.get("status"))) {
            throw new RuntimeException("只有已完成状态的单据可反审核");
        }
        List<Map<String, Object>> items = storage.findOutboundItemsByOrderId(orderId);
        for (Map<String, Object> item : items) {
            BigDecimal shippedQty = toBigDecimal(item.get("shippedQty"));
            if (shippedQty.compareTo(BigDecimal.ZERO) <= 0) continue;
            Long materialId = toLong(item.get("materialId"));
            List<Map<String, Object>> invs = storage.findAllInventories().stream()
                    .filter(inv -> Objects.equals(toLong(inv.get("materialId")), materialId))
                    .filter(inv -> toBigDecimal(inv.get("qty")).compareTo(BigDecimal.ZERO) >= 0)
                    .collect(Collectors.toList());
            if (!invs.isEmpty()) {
                Map<String, Object> inv = invs.get(0);
                BigDecimal newQty = toBigDecimal(inv.get("qty")).add(shippedQty);
                inv.put("qty", newQty);
                storage.saveInventory(inv);
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("recordNo", generateNo("LS"));
                record.put("businessType", "出库反审");
                record.put("businessNo", order.get("orderNo"));
                record.put("materialId", materialId);
                record.put("materialCode", item.get("materialCode"));
                record.put("materialName", item.get("materialName"));
                record.put("batchNo", safeToString(inv.get("batchNo")));
                record.put("changeQty", shippedQty);
                record.put("beforeQty", toBigDecimal(inv.get("qty")).subtract(shippedQty));
                record.put("afterQty", newQty);
                record.put("operator", operator);
                record.put("operateTime", now());
                record.put("remark", "出库反审核-" + reason);
                storage.saveRecord(record);
            }
            item.put("shippedQty", BigDecimal.ZERO);
            item.put("status", "待出库");
            storage.saveOutboundItem(item);
        }
        order.put("status", "已撤销");
        order.put("reverseReason", reason);
        storage.saveOutboundOrder(order);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "反审核成功");
        result.put("orderNo", order.get("orderNo"));
        return result;
    }

    // ==================== 拣货指引 ====================
    public List<Map<String, Object>> getPickingTasks(Long orderId) {
        List<Map<String, Object>> items = storage.findOutboundItemsByOrderId(orderId);
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (toBigDecimal(item.get("shippedQty")).compareTo(toBigDecimal(item.get("planQty"))) >= 0) continue;
            Long materialId = toLong(item.get("materialId"));
            List<Map<String, Object>> invs = storage.findAllInventories().stream()
                    .filter(inv -> Objects.equals(toLong(inv.get("materialId")), materialId))
                    .filter(inv -> availableQty(inv).compareTo(BigDecimal.ZERO) > 0)
                    .sorted((a, b) -> safeToString(a.get("batchNo")).compareTo(safeToString(b.get("batchNo"))))
                    .collect(Collectors.toList());
            for (Map<String, Object> inv : invs) {
                Map<String, Object> task = new LinkedHashMap<>();
                task.put("materialCode", item.get("materialCode"));
                task.put("materialName", item.get("materialName"));
                task.put("batchNo", inv.get("batchNo"));
                task.put("warehouseName", inv.get("warehouseName"));
                task.put("locationName", inv.get("locationName"));
                task.put("availableQty", availableQty(inv));
                task.put("needQty", item.get("planQty"));
                tasks.add(task);
            }
        }
        return tasks;
    }

    // ==================== 辅助方法 ====================

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

    private String safeToString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private BigDecimal getTotalInventoryForMaterial(Long materialId) {
        return storage.findAllInventories().stream()
                .filter(inv -> Objects.equals(toLong(inv.get("materialId")), materialId))
                .map(inv -> toBigDecimal(inv.get("qty")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal availableQty(Map<String, Object> inv) {
        return toBigDecimal(inv.get("qty")).subtract(toBigDecimal(inv.get("frozenQty"))).max(BigDecimal.ZERO);
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + new Random().nextInt(1000);
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ==================== 扫码出库 ====================
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> shipOutbound(Map<String, Object> body) {
        String kanbanCode = (String) body.get("kanbanCode");
        BigDecimal shipQty = new BigDecimal(body.get("shipQty").toString());
        String operator = (String) body.getOrDefault("operator", "admin");

        if (shipQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("出库数量必须大于0");
        }

        // 查找看板
        Map<String, Object> kanban = storage.findKanbanByCode(kanbanCode);
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanCode);

        BigDecimal receivedQty = toBigDecimal(kanban.get("receivedQty"));
        if (shipQty.compareTo(receivedQty) > 0) {
            throw new RuntimeException("出库数量(" + shipQty + ")超过已入库数量(" + receivedQty + ")");
        }

        // 更新看板：扣减已入库数量
        BigDecimal newReceived = receivedQty.subtract(shipQty);
        kanban.put("receivedQty", newReceived);
        if (newReceived.compareTo(BigDecimal.ZERO) <= 0) {
            kanban.put("status", "已出库");
        } else {
            kanban.put("status", "部分出库");
        }
        storage.saveKanban(kanban);

        // 更新库存：扣减
        Long materialId = toLong(kanban.get("materialId"));
        Long warehouseId = toLong(kanban.get("warehouseId"));
        Long locationId = toLong(kanban.get("locationId"));
        String batchNo = (String) kanban.get("batchNo");
        Map<String, Object> inventory = storage.findInventoryByKey(materialId, warehouseId, locationId, batchNo);
        BigDecimal beforeQty = BigDecimal.ZERO;
        if (inventory != null) {
            beforeQty = toBigDecimal(inventory.get("qty"));
            if (shipQty.compareTo(availableQty(inventory)) > 0) {
                throw new RuntimeException("当前库存存在封存数量，可出库数量不足");
            }
            BigDecimal afterQty = beforeQty.subtract(shipQty);
            inventory.put("qty", afterQty.max(BigDecimal.ZERO));
            inventory.put("status", afterQty.compareTo(BigDecimal.ZERO) <= 0 ? "已耗尽" : "已入库");
            storage.saveInventory(inventory);
        }

        // 更新关联的出库单明细
        Long orderId = body.get("orderId") != null ? toLong(body.get("orderId")) : null;
        if (orderId != null) {
            List<Map<String, Object>> outItems = storage.findOutboundItemsByOrderId(orderId);
            if (outItems != null) {
                for (Map<String, Object> oi : outItems) {
                    if (Objects.equals(toLong(oi.get("materialId")), materialId)) {
                        BigDecimal curShipped = toBigDecimal(oi.get("shippedQty"));
                        BigDecimal newShipped = curShipped.add(shipQty);
                        oi.put("shippedQty", newShipped);
                        BigDecimal oiPlan = toBigDecimal(oi.get("planQty"));
                        if (newShipped.compareTo(oiPlan) >= 0) {
                            oi.put("status", "已完成");
                        } else {
                            oi.put("status", "部分出库");
                        }
                        storage.saveOutboundItem(oi);
                    }
                }
            }
            // 刷新出库单状态
            refreshOutboundOrderStatus(orderId);
        }

        // 记录流水
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("recordNo", generateNo("CK"));
        record.put("businessType", "出库");
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
        record.put("changeQty", shipQty.negate());
        record.put("beforeQty", beforeQty);
        record.put("afterQty", beforeQty.subtract(shipQty).max(BigDecimal.ZERO));
        record.put("operator", operator);
        record.put("operateTime", now());
        record.put("remark", "扫码出库");
        storage.saveRecord(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kanban", kanban);
        result.put("inventory", inventory);
        result.put("record", record);
        return result;
    }

}
