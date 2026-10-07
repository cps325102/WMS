package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FreezeService {
    private final DataStorage storage;
    public FreezeService(DataStorage storage) { this.storage = storage; }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> freeze(Map<String, Object> body) {
        Long inventoryId = toLong(body.get("inventoryId"));
        BigDecimal freezeQty = toBigDecimal(body.get("freezeQty"));
        String reason = safeToString(body.get("freezeReason"));
        String operator = safeToString(body.getOrDefault("operator", "admin"));
        if (freezeQty.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("封存数量必须大于0");

        Map<String, Object> inv = null;
        if (inventoryId != null) {
            inv = storage.findAllInventories().stream()
                    .filter(i -> Objects.equals(toLong(i.get("id")), inventoryId))
                    .findFirst().orElse(null);
        }
        Long materialId = inv == null ? toLong(body.get("materialId")) : toLong(inv.get("materialId"));
        Long warehouseId = inv == null ? toLong(body.get("warehouseId")) : toLong(inv.get("warehouseId"));
        Long locationId = inv == null ? toLong(body.get("locationId")) : toLong(inv.get("locationId"));
        String batchNo = inv == null ? safeToString(body.get("batchNo")) : safeToString(inv.get("batchNo"));
        if (inv == null) inv = storage.findInventoryByKey(materialId, warehouseId, locationId, batchNo);
        if (inv == null) throw new RuntimeException("未找到对应库存记录");
        BigDecimal currentQty = toBigDecimal(inv.get("qty"));
        BigDecimal currentFrozen = toBigDecimal(inv.get("frozenQty"));
        if (freezeQty.compareTo(currentQty.subtract(currentFrozen)) > 0) throw new RuntimeException("可封存数量不足");

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("freezeNo", storage.generateNo("FC"));
        record.put("inventoryId", toLong(inv.get("id")));
        record.put("materialId", materialId);
        record.put("materialCode", inv.get("materialCode"));
        record.put("materialName", inv.get("materialName"));
        record.put("warehouseId", warehouseId);
        record.put("warehouseName", inv.get("warehouseName"));
        record.put("locationId", locationId);
        record.put("locationName", inv.get("locationName"));
        record.put("batchNo", batchNo);
        record.put("freezeQty", freezeQty);
        record.put("beforeFrozenQty", currentFrozen);
        record.put("afterFrozenQty", currentFrozen.add(freezeQty));
        record.put("unfrozenQty", BigDecimal.ZERO);
        record.put("freezeReason", reason);
        record.put("operator", operator);
        record.put("status", "已冻结");
        Map<String, Object> savedRecord = storage.saveFreezeRecord(record);

        inv.put("frozenQty", currentFrozen.add(freezeQty));
        storage.saveInventory(inv);

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("recordNo", storage.generateNo("LS"));
        log.put("businessType", "封存");
        log.put("materialId", materialId);
        log.put("materialCode", inv.get("materialCode"));
        log.put("materialName", inv.get("materialName"));
        log.put("warehouseId", warehouseId);
        log.put("warehouseName", inv.get("warehouseName"));
        log.put("locationId", locationId);
        log.put("locationName", inv.get("locationName"));
        log.put("batchNo", batchNo);
        log.put("changeQty", BigDecimal.ZERO);
        log.put("operator", operator);
        log.put("operateTime", now());
        log.put("remark", "封存-" + reason);
        storage.saveRecord(log);
        return savedRecord;
    }

    public List<Map<String, Object>> listFreezeRecords(String keyword, String status) {
        List<Map<String, Object>> list = storage.findAllFreezeRecords();
        if (keyword != null && !keyword.isBlank()) {
            String lower = keyword.toLowerCase();
            list = list.stream().filter(r -> safeToString(r.get("materialCode")).toLowerCase().contains(lower)
                    || safeToString(r.get("materialName")).toLowerCase().contains(lower)
                    || safeToString(r.get("batchNo")).toLowerCase().contains(lower)).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(r -> status.equals(r.get("status"))).collect(Collectors.toList());
        }
        return list;
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> r = storage.findFreezeRecordById(id);
        if (r == null) throw new RuntimeException("封存记录不存在");
        r.put("unfreezeRecords", storage.findUnfreezeRecordsByFreezeId(id));
        return r;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> unfreeze(Long freezeId, BigDecimal unfreezeQty, String reason, String operator) {
        Map<String, Object> freeze = storage.findFreezeRecordById(freezeId);
        if (freeze == null) throw new RuntimeException("封存记录不存在");
        if (unfreezeQty == null || unfreezeQty.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("解封数量必须大于0");
        BigDecimal currentFrozen = toBigDecimal(freeze.get("afterFrozenQty"));
        BigDecimal alreadyUnfrozen = toBigDecimal(freeze.get("unfrozenQty"));
        BigDecimal canUnfreeze = currentFrozen.subtract(alreadyUnfrozen);
        if (unfreezeQty.compareTo(canUnfreeze) > 0) throw new RuntimeException("解封数量超过可解封数量");

        Map<String, Object> unfreeze = new LinkedHashMap<>();
        unfreeze.put("freezeId", freezeId);
        unfreeze.put("unfreezeQty", unfreezeQty);
        unfreeze.put("unfreezeReason", reason);
        unfreeze.put("operator", operator);
        storage.saveUnfreezeRecord(unfreeze);

        freeze.put("unfrozenQty", alreadyUnfrozen.add(unfreezeQty));
        if (alreadyUnfrozen.add(unfreezeQty).compareTo(currentFrozen) >= 0) freeze.put("status", "已解冻");
        storage.saveFreezeRecord(freeze);

        Long materialId = toLong(freeze.get("materialId"));
        Long warehouseId = toLong(freeze.get("warehouseId"));
        Long locationId = toLong(freeze.get("locationId"));
        String batchNo = safeToString(freeze.get("batchNo"));
        Map<String, Object> inv = storage.findInventoryByKey(materialId, warehouseId, locationId, batchNo);
        if (inv != null) {
            inv.put("frozenQty", toBigDecimal(inv.get("frozenQty")).subtract(unfreezeQty).max(BigDecimal.ZERO));
            storage.saveInventory(inv);
        }

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("recordNo", storage.generateNo("LS"));
        log.put("businessType", "解封");
        log.put("materialId", materialId);
        log.put("materialCode", freeze.get("materialCode"));
        log.put("materialName", freeze.get("materialName"));
        log.put("warehouseId", warehouseId);
        log.put("warehouseName", freeze.get("warehouseName"));
        log.put("locationId", locationId);
        log.put("locationName", freeze.get("locationName"));
        log.put("batchNo", batchNo);
        log.put("changeQty", BigDecimal.ZERO);
        log.put("operator", operator);
        log.put("operateTime", now());
        log.put("remark", "解封-" + reason);
        storage.saveRecord(log);
        return unfreeze;
    }

    public List<Map<String, Object>> listRepackRecords(String keyword) {
        List<Map<String, Object>> list = storage.findAllRepackRecords();
        if (keyword == null || keyword.isBlank()) return list;
        String lower = keyword.toLowerCase();
        return list.stream().filter(r -> safeToString(r.get("materialCode")).toLowerCase().contains(lower)
                || safeToString(r.get("materialName")).toLowerCase().contains(lower)
                || safeToString(r.get("fromBatchNo")).toLowerCase().contains(lower)
                || safeToString(r.get("toBatchNo")).toLowerCase().contains(lower)).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> repack(Map<String, Object> body) {
        Long inventoryId = toLong(body.get("inventoryId"));
        BigDecimal repackQty = toBigDecimal(body.get("repackQty"));
        Long toWarehouseId = toLong(body.get("toWarehouseId"));
        Long toLocationId = toLong(body.get("toLocationId"));
        String toBatchNo = safeToString(body.get("toBatchNo"));
        BigDecimal packageQty = toBigDecimal(body.get("packageQty"));
        String operator = safeToString(body.getOrDefault("operator", "admin"));
        String reason = safeToString(body.getOrDefault("reason", "转包"));
        if (inventoryId == null) throw new RuntimeException("请选择源库存");
        if (repackQty.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("转包数量必须大于0");

        Map<String, Object> source = storage.findAllInventories().stream()
                .filter(i -> Objects.equals(toLong(i.get("id")), inventoryId))
                .findFirst().orElse(null);
        if (source == null) throw new RuntimeException("源库存不存在");
        BigDecimal qty = toBigDecimal(source.get("qty"));
        BigDecimal frozen = toBigDecimal(source.get("frozenQty"));
        if (repackQty.compareTo(qty.subtract(frozen)) > 0) throw new RuntimeException("可转包数量不足");

        Map<String, Object> warehouse = toWarehouseId == null ? null : storage.findWarehouseById(toWarehouseId);
        Map<String, Object> location = toLocationId == null ? null : storage.findLocationById(toLocationId);
        if (toWarehouseId == null) toWarehouseId = toLong(source.get("warehouseId"));
        if (toLocationId == null) toLocationId = toLong(source.get("locationId"));
        String targetBatch = toBatchNo.isBlank() ? safeToString(source.get("batchNo")) : toBatchNo;

        source.put("qty", qty.subtract(repackQty));
        storage.saveInventory(source);

        Map<String, Object> target = storage.findInventoryByKey(toLong(source.get("materialId")), toWarehouseId, toLocationId, targetBatch);
        if (target == null) {
            target = new LinkedHashMap<>();
            target.put("materialId", source.get("materialId"));
            target.put("materialCode", source.get("materialCode"));
            target.put("materialName", source.get("materialName"));
            target.put("warehouseId", toWarehouseId);
            target.put("warehouseName", warehouse == null ? source.get("warehouseName") : warehouse.get("name"));
            target.put("locationId", toLocationId);
            target.put("locationName", location == null ? source.get("locationName") : location.get("name"));
            target.put("batchNo", targetBatch);
            target.put("qty", BigDecimal.ZERO);
            target.put("frozenQty", BigDecimal.ZERO);
            target.put("unit", source.get("unit"));
        }
        target.put("qty", toBigDecimal(target.get("qty")).add(repackQty));
        if (packageQty.compareTo(BigDecimal.ZERO) > 0) target.put("packageQty", packageQty);
        Map<String, Object> savedTarget = storage.saveInventory(target);

        Map<String, Object> repack = new LinkedHashMap<>();
        repack.put("repackNo", storage.generateNo("ZB"));
        repack.put("sourceInventoryId", inventoryId);
        repack.put("targetInventoryId", savedTarget.get("id"));
        repack.put("materialId", source.get("materialId"));
        repack.put("materialCode", source.get("materialCode"));
        repack.put("materialName", source.get("materialName"));
        repack.put("fromWarehouseName", source.get("warehouseName"));
        repack.put("fromLocationName", source.get("locationName"));
        repack.put("toWarehouseId", toWarehouseId);
        repack.put("toWarehouseName", savedTarget.get("warehouseName"));
        repack.put("toLocationId", toLocationId);
        repack.put("toLocationName", savedTarget.get("locationName"));
        repack.put("fromBatchNo", source.get("batchNo"));
        repack.put("toBatchNo", targetBatch);
        repack.put("repackQty", repackQty);
        repack.put("beforeQty", qty);
        repack.put("afterQty", qty.subtract(repackQty));
        repack.put("packageQty", packageQty);
        repack.put("operator", operator);
        repack.put("reason", reason);
        Map<String, Object> saved = storage.saveRepackRecord(repack);

        Map<String, Object> log = new LinkedHashMap<>();
        log.put("recordNo", storage.generateNo("LS"));
        log.put("businessType", "转包");
        log.put("materialId", source.get("materialId"));
        log.put("materialCode", source.get("materialCode"));
        log.put("materialName", source.get("materialName"));
        log.put("batchNo", source.get("batchNo") + " -> " + targetBatch);
        log.put("changeQty", BigDecimal.ZERO);
        log.put("beforeQty", qty);
        log.put("afterQty", qty.subtract(repackQty));
        log.put("operator", operator);
        log.put("operateTime", now());
        log.put("remark", reason);
        storage.saveRecord(log);
        return saved;
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
