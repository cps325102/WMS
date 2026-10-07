package com.example.wms.service;

import com.example.wms.storage.DataStorage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BasicDataService {
    private final DataStorage storage;

    public BasicDataService(DataStorage storage) {
        this.storage = storage;
    }

    public List<Map<String, Object>> listMaterials(String keyword) {
        return filter(storage.findAllMaterials(), keyword);
    }
    public Map<String, Object> saveMaterial(Map<String, Object> row) {
        return storage.saveMaterial(row);
    }
    public void deleteMaterial(Long id) {
        try {
            storage.deleteMaterial(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("该物料已被入库单、出库单或库存引用，不能删除");
        }
    }

    public List<Map<String, Object>> listSuppliers(String keyword) {
        return filter(storage.findAllSuppliers(), keyword);
    }
    public Map<String, Object> saveSupplier(Map<String, Object> row) {
        return storage.saveSupplier(row);
    }
    public void deleteSupplier(Long id) {
        try {
            storage.deleteSupplier(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("该供应商仍被物料或入库单引用，不能删除");
        }
    }

    public List<Map<String, Object>> listCustomers(String keyword) {
        return filter(storage.findAllCustomers(), keyword);
    }
    public Map<String, Object> saveCustomer(Map<String, Object> row) {
        return storage.saveCustomer(row);
    }
    public void deleteCustomer(Long id) {
        try {
            storage.deleteCustomer(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("该客户仍被出库单引用，不能删除");
        }
    }

    public List<Map<String, Object>> listWarehouses(String keyword) {
        return filter(storage.findAllWarehouses(), keyword);
    }
    public Map<String, Object> saveWarehouse(Map<String, Object> row) {
        return storage.saveWarehouse(row);
    }
    public void deleteWarehouse(Long id) {
        try {
            storage.deleteWarehouse(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("该仓库下仍有库存，不能删除；请先清空库存或改由库位层面处理");
        }
    }

    public List<Map<String, Object>> listLocations(String keyword) {
        return filter(storage.findAllLocations(), keyword);
    }
    public Map<String, Object> saveLocation(Map<String, Object> row) {
        return storage.saveLocation(row);
    }
    public void deleteLocation(Long id) {
        try {
            storage.deleteLocation(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("该库位上仍有库存，不能删除");
        }
    }

    private List<Map<String, Object>> filter(List<Map<String, Object>> list, String keyword) {
        if (keyword == null || keyword.isBlank()) return list;
        String lower = keyword.toLowerCase();
        return list.stream()
                .filter(m -> m.values().stream().anyMatch(v -> v != null && v.toString().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }
}
