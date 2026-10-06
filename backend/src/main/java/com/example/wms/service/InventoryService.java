package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private final DataStorage storage;

    public InventoryService(DataStorage storage) {
        this.storage = storage;
    }

    public List<Map<String, Object>> listInventory(String keyword) {
        List<Map<String, Object>> list = storage.findAllInventories();
        if (keyword == null || keyword.isBlank()) return list;
        String lower = keyword.toLowerCase();
        return list.stream()
                .filter(i -> {
                    String materialCode = i.get("materialCode") == null ? "" : i.get("materialCode").toString().toLowerCase();
                    String materialName = i.get("materialName") == null ? "" : i.get("materialName").toString().toLowerCase();
                    String warehouseName = i.get("warehouseName") == null ? "" : i.get("warehouseName").toString().toLowerCase();
                    String locationName = i.get("locationName") == null ? "" : i.get("locationName").toString().toLowerCase();
                    String batchNo = i.get("batchNo") == null ? "" : i.get("batchNo").toString().toLowerCase();
                    return materialCode.contains(lower) || materialName.contains(lower) ||
                            warehouseName.contains(lower) || locationName.contains(lower) ||
                            batchNo.contains(lower);
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> trace(String keyword) {
        List<Map<String, Object>> records = storage.findAllRecords();
        if (keyword == null || keyword.isBlank()) return records;
        String lower = keyword.toLowerCase();
        return records.stream()
                .filter(r -> {
                    String recordNo = r.get("recordNo") == null ? "" : r.get("recordNo").toString().toLowerCase();
                    String businessNo = r.get("businessNo") == null ? "" : r.get("businessNo").toString().toLowerCase();
                    String kanbanCode = r.get("kanbanCode") == null ? "" : r.get("kanbanCode").toString().toLowerCase();
                    String materialCode = r.get("materialCode") == null ? "" : r.get("materialCode").toString().toLowerCase();
                    String materialName = r.get("materialName") == null ? "" : r.get("materialName").toString().toLowerCase();
                    String warehouseName = r.get("warehouseName") == null ? "" : r.get("warehouseName").toString().toLowerCase();
                    String locationName = r.get("locationName") == null ? "" : r.get("locationName").toString().toLowerCase();
                    String batchNo = r.get("batchNo") == null ? "" : r.get("batchNo").toString().toLowerCase();
                    String operator = r.get("operator") == null ? "" : r.get("operator").toString().toLowerCase();
                    return recordNo.contains(lower) || businessNo.contains(lower) || kanbanCode.contains(lower) ||
                            materialCode.contains(lower) || materialName.contains(lower) ||
                            warehouseName.contains(lower) || locationName.contains(lower) ||
                            batchNo.contains(lower) || operator.contains(lower);
                })
                .collect(Collectors.toList());
    }
}