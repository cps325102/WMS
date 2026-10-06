package com.example.wms.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DataStorage {
    private final JdbcTemplate db;
    private final AtomicLong idGen = new AtomicLong(10000);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DataStorage(JdbcTemplate db) {
        this.db = db;
        // 从数据库中读取最大 ID，确保不会冲突
        Long maxId = 0L;
        try {
            String[] tables = {"materials","suppliers","customers","warehouses","locations",
                "inbound_orders","inbound_order_items","kanbans","inventories","records",
                "outbound_orders","outbound_order_items","users","freeze_records","unfreeze_records",
                "repack_records","alert_logs","alert_configs","allocations"};
            for (String t : tables) {
                try {
                    Long m = db.queryForObject("SELECT COALESCE(MAX(id),0) FROM " + t, Long.class);
                    if (m != null && m > maxId) maxId = m;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        idGen.set(Math.max(maxId + 100, 20000));
    }

    private Long nextId() { return idGen.incrementAndGet(); }
    private String now() { return LocalDateTime.now().format(DTF); }

    // ===== 基础数据 =====
    public List<Map<String, Object>> findAllMaterials() { return list("SELECT * FROM materials ORDER BY id"); }
    public Map<String, Object> findMaterialById(Long id) { return one("SELECT * FROM materials WHERE id="+id); }
    public Map<String, Object> saveMaterial(Map<String, Object> row) { return save("materials", row, "code,name,spec,unit,package_qty,category,min_stock,max_stock".split(",")); }
    public void deleteMaterial(Long id) { db.update("DELETE FROM materials WHERE id="+id); }

    public List<Map<String, Object>> findAllSuppliers() { return list("SELECT * FROM suppliers ORDER BY id"); }
    public Map<String, Object> findSupplierById(Long id) { return one("SELECT * FROM suppliers WHERE id="+id); }
    public Map<String, Object> saveSupplier(Map<String, Object> row) { return save("suppliers", row, "code,name,contact_name,contact_phone,address,province,city".split(",")); }
    public void deleteSupplier(Long id) { db.update("DELETE FROM suppliers WHERE id="+id); }

    public List<Map<String, Object>> findAllCustomers() { return list("SELECT * FROM customers ORDER BY id"); }
    public Map<String, Object> findCustomerById(Long id) { return one("SELECT * FROM customers WHERE id="+id); }
    public Map<String, Object> saveCustomer(Map<String, Object> row) { return save("customers", row, "code,name,contact_name,contact_phone,address,province,city".split(",")); }
    public void deleteCustomer(Long id) { db.update("DELETE FROM customers WHERE id="+id); }

    public List<Map<String, Object>> findAllWarehouses() { return list("SELECT * FROM warehouses ORDER BY id"); }
    public Map<String, Object> findWarehouseById(Long id) { return one("SELECT * FROM warehouses WHERE id="+id); }
    public Map<String, Object> saveWarehouse(Map<String, Object> row) { return save("warehouses", row, "code,name,address,city,type".split(",")); }
    public void deleteWarehouse(Long id) { db.update("DELETE FROM warehouses WHERE id="+id); }

    public List<Map<String, Object>> findAllLocations() { return list("SELECT * FROM locations ORDER BY id"); }
    public Map<String, Object> findLocationById(Long id) { return one("SELECT * FROM locations WHERE id="+id); }
    public Map<String, Object> saveLocation(Map<String, Object> row) { return save("locations", row, "warehouse_id,code,name,area,shelf,layer".split(",")); }
    public void deleteLocation(Long id) { db.update("DELETE FROM locations WHERE id="+id); }

    // ===== 入库单 & 明细 =====
    public List<Map<String, Object>> findAllOrders() { return list("SELECT * FROM inbound_orders ORDER BY created_at DESC"); }
    public Map<String, Object> findOrderById(Long id) { return one("SELECT * FROM inbound_orders WHERE id="+id); }
    public Map<String, Object> saveOrder(Map<String, Object> row) { return save("inbound_orders", row, "order_no,supplier_id,warehouse_id,status,expected_date,remark".split(",")); }
    public void deleteOrder(Long id) { db.update("DELETE FROM inbound_orders WHERE id="+id); }

    public List<Map<String, Object>> findItemsByOrderId(Long orderId) {
        return list("SELECT * FROM inbound_order_items WHERE order_id="+orderId);
    }
    public Map<String, Object> findItemById(Long id) { return one("SELECT * FROM inbound_order_items WHERE id="+id); }
    public Map<String, Object> saveItem(Map<String, Object> row) { return save("inbound_order_items", row, "order_id,material_id,plan_qty,received_qty,batch_no,location_id,warehouse_id,material_code,material_name,spec,unit,warehouse_name,location_name,status".split(",")); }
    public void deleteItem(Long id) { db.update("DELETE FROM inbound_order_items WHERE id="+id); }
    public void deleteItemsByOrderId(Long orderId) { db.update("DELETE FROM inbound_order_items WHERE order_id="+orderId); }

    // ===== 看板 =====
    public List<Map<String, Object>> findAllKanbans() { return list("SELECT * FROM kanbans ORDER BY created_at DESC"); }
    public Map<String, Object> findKanbanById(Long id) { return one("SELECT * FROM kanbans WHERE id="+id); }
    public Map<String, Object> findKanbanByCode(String code) { return one("SELECT * FROM kanbans WHERE kanban_code='"+code+"'"); }
    public Map<String, Object> saveKanban(Map<String, Object> row) {
        return save("kanbans", row, "kanban_code,order_id,order_item_id,material_id,order_no,material_code,material_name,plan_qty,received_qty,warehouse_id,warehouse_name,location_id,location_name,batch_no,status,print_count,package_info,last_print_time".split(","));
    }
    public void deleteKanban(Long id) { db.update("DELETE FROM kanbans WHERE id="+id); }
    public void deleteKanbansByOrderId(Long orderId) { db.update("DELETE FROM kanbans WHERE order_id="+orderId); }

    // ===== 库存 =====
    public List<Map<String, Object>> findAllInventories() { return list("SELECT * FROM inventories ORDER BY updated_at DESC"); }
    public Map<String, Object> findInventoryByKey(Long materialId, Long warehouseId, Long locationId, String batchNo) {
        return one("SELECT * FROM inventories WHERE material_id="+materialId+" AND warehouse_id="+warehouseId+" AND location_id="+locationId+" AND batch_no='"+batchNo+"'");
    }
    public Map<String, Object> saveInventory(Map<String, Object> row) {
        Object qtyObj = row.get("qty");
        if (qtyObj != null) {
            BigDecimal qty = new BigDecimal(qtyObj.toString());
            row.put("status", qty.compareTo(BigDecimal.ZERO) == 0 ? "已耗尽" : "已入库");
        }
        return save("inventories", row, "material_id,warehouse_id,location_id,material_code,material_name,warehouse_name,location_name,batch_no,qty,frozen_qty,package_qty,unit,status".split(","));
    }
    public void deleteInventory(Long id) { db.update("DELETE FROM inventories WHERE id="+id); }

    // ===== 流水记录 =====
    public List<Map<String, Object>> findAllRecords() { return list("SELECT * FROM records ORDER BY operate_time DESC"); }
    public Map<String, Object> saveRecord(Map<String, Object> row) {
        return save("records", row, "record_no,business_type,business_no,kanban_code,material_id,material_code,material_name,warehouse_id,warehouse_name,location_id,location_name,batch_no,change_qty,before_qty,after_qty,operator,remark,operate_time".split(","));
    }

    // ===== 出库单 & 明细 =====
    public List<Map<String, Object>> findAllOutboundOrders() { return list("SELECT * FROM outbound_orders ORDER BY created_at DESC"); }
    public Map<String, Object> findOutboundOrderById(Long id) { return one("SELECT * FROM outbound_orders WHERE id="+id); }
    public Map<String, Object> saveOutboundOrder(Map<String, Object> row) { return save("outbound_orders", row, "order_no,customer_id,warehouse_id,status,plan_date,remark".split(",")); }
    public void deleteOutboundOrder(Long id) { db.update("DELETE FROM outbound_orders WHERE id="+id); }

    public List<Map<String, Object>> findAllOutboundItems() { return list("SELECT * FROM outbound_order_items ORDER BY id"); }
    public List<Map<String, Object>> findOutboundItemsByOrderId(Long orderId) { return list("SELECT * FROM outbound_order_items WHERE order_id="+orderId); }
    public Map<String, Object> findOutboundItemById(Long id) { return one("SELECT * FROM outbound_order_items WHERE id="+id); }
    public Map<String, Object> saveOutboundItem(Map<String, Object> row) { return save("outbound_order_items", row, "order_id,material_id,plan_qty,shipped_qty,batch_no".split(",")); }
    public void deleteOutboundItem(Long id) { db.update("DELETE FROM outbound_order_items WHERE id="+id); }
    public void deleteOutboundItemsByOrderId(Long orderId) { db.update("DELETE FROM outbound_order_items WHERE order_id="+orderId); }

    // ===== 分配/拣货/冻结 =====
    public List<Map<String, Object>> findAllAllocations() { return list("SELECT * FROM allocations ORDER BY id"); }
    public List<Map<String, Object>> findAllocationsByOrderId(Long orderId) { return list("SELECT * FROM allocations WHERE order_id="+orderId); }
    public Map<String, Object> saveAllocation(Map<String, Object> row) { return save("allocations", row, "order_id,order_item_id,inventory_id,material_code,material_name,batch_no,alloc_qty,warehouse_name,location_name".split(",")); }

    public List<Map<String, Object>> findAllPickRecords() { return list("SELECT * FROM allocations WHERE 1=0"); } // stub
    public List<Map<String, Object>> findPickRecordsByOrderId(Long oid) { return list("SELECT * FROM allocations WHERE order_id="+oid); }
    public Map<String, Object> savePickRecord(Map<String, Object> row) { return saveAllocation(row); }

    public List<Map<String, Object>> findAllFreezeRecords() { return list("SELECT * FROM freeze_records ORDER BY created_at DESC"); }
    public Map<String, Object> findFreezeRecordById(Long id) { return one("SELECT * FROM freeze_records WHERE id="+id); }
    public Map<String, Object> saveFreezeRecord(Map<String, Object> row) {
        return save("freeze_records", row, "freeze_no,inventory_id,material_id,warehouse_id,warehouse_name,location_id,location_name,material_code,material_name,batch_no,freeze_qty,before_frozen_qty,after_frozen_qty,unfrozen_qty,freeze_reason,operator,status".split(","));
    }

    public List<Map<String, Object>> findAllUnfreezeRecords() { return list("SELECT * FROM unfreeze_records ORDER BY created_at DESC"); }
    public List<Map<String, Object>> findUnfreezeRecordsByFreezeId(Long fid) { return list("SELECT * FROM unfreeze_records WHERE freeze_id="+fid+" ORDER BY created_at DESC"); }
    public Map<String, Object> saveUnfreezeRecord(Map<String, Object> row) {
        return save("unfreeze_records", row, "freeze_id,unfreeze_qty,unfreeze_reason,operator".split(","));
    }

    public List<Map<String, Object>> findAllRepackRecords() { return list("SELECT * FROM repack_records ORDER BY created_at DESC"); }
    public Map<String, Object> saveRepackRecord(Map<String, Object> row) {
        return save("repack_records", row, "repack_no,source_inventory_id,target_inventory_id,material_id,material_code,material_name,from_warehouse_name,from_location_name,to_warehouse_id,to_warehouse_name,to_location_id,to_location_name,from_batch_no,to_batch_no,repack_qty,before_qty,after_qty,package_qty,operator,reason".split(","));
    }

    public List<Map<String, Object>> findAllInboundRecords() { return list("SELECT * FROM records WHERE business_type='入库' ORDER BY operate_time DESC"); }
    public List<Map<String, Object>> findInboundRecordsByOrderId(Long oid) { return list("SELECT * FROM records WHERE business_no IN (SELECT order_no FROM inbound_orders WHERE id="+oid+")"); }
    public Map<String, Object> saveInboundRecord(Map<String, Object> row) { return saveRecord(row); }

    // ===== 预警/每日汇总/PDA/用户/FIFO =====
    public List<Map<String, Object>> findAllAlertLogs() { return list("SELECT * FROM alert_logs ORDER BY created_at DESC"); }
    public Map<String, Object> findAlertLogById(Long id) { return one("SELECT * FROM alert_logs WHERE id="+id); }
    public Map<String, Object> findOpenAlert(String alertType, Long materialId, String batchNo) {
        String batch = batchNo == null ? "" : batchNo.replace("'", "''");
        return one("SELECT * FROM alert_logs WHERE alert_type='" + alertType + "' AND material_id=" + materialId
                + " AND COALESCE(batch_no,'')='" + batch + "' AND status='待处理'");
    }
    public Map<String, Object> saveAlertLog(Map<String, Object> row) {
        return save("alert_logs", row, "alert_no,alert_type,level,status,material_id,material_code,material_name,warehouse_name,location_name,batch_no,qty,available_qty,threshold_qty,daily_avg_out,lead_time_days,stagnant_days,message,suggestion,handler,action,remark,handled_at".split(","));
    }

    public List<Map<String, Object>> findAllAlertConfigs() { return list("SELECT * FROM alert_configs ORDER BY id"); }
    public Map<String, Object> findAlertConfigById(Long id) { return one("SELECT * FROM alert_configs WHERE id="+id); }
    public Map<String, Object> saveAlertConfig(Map<String, Object> row) { return save("alert_configs", row, "material_id,lead_time_days,stagnant_days,enabled".split(",")); }
    public void deleteAlertConfig(Long id) { db.update("DELETE FROM alert_configs WHERE id="+id); }

    public List<Map<String, Object>> findAllDailySummaries() { return list("SELECT * FROM records WHERE 1=0"); }
    public Map<String, Object> saveDailySummary(Map<String, Object> row) { return saveRecord(row); }

    public List<Map<String, Object>> findAllPdaCaches() { return new ArrayList<>(); }
    public Map<String, Object> savePdaCache(Map<String, Object> row) { return row; }
    public void deletePdaCache(Long id) {}

    public List<Map<String, Object>> findAllUsers() { return list("SELECT * FROM users ORDER BY id"); }
    public Map<String, Object> findUserByUsername(String username) { return one("SELECT * FROM users WHERE username='"+username+"'"); }
    public Map<String, Object> saveUser(Map<String, Object> row) { return save("users", row, "username,password,nickname,role".split(",")); }

    public List<Map<String, Object>> findAllFifoConfigs() { return new ArrayList<>(); }
    public Map<String, Object> findFifoConfigById(Long id) { return null; }
    public Map<String, Object> saveFifoConfig(Map<String, Object> row) { return row; }

    public String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + new Random().nextInt(1000);
    }

    // ===== 数据库操作方法 =====
    private List<Map<String, Object>> list(String sql) {
        return db.query(sql, (rs, i) -> rowFromRs(rs));
    }

    private Map<String, Object> one(String sql) {
        List<Map<String, Object>> list = db.query(sql, (rs, i) -> rowFromRs(rs));
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Object> rowFromRs(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String name = meta.getColumnName(i);
            Object val = rs.getObject(i);
            row.put(name, val);
            // 同时添加 camelCase 版本（如 plan_qty -> planQty）
            if (name.contains("_")) {
                StringBuilder camel = new StringBuilder();
                boolean up = false;
                for (char c : name.toCharArray()) {
                    if (c == '_') { up = true; continue; }
                    camel.append(up ? Character.toUpperCase(c) : c);
                    up = false;
                }
                row.put(camel.toString(), val);
            }
        }
        return row;
    }

    // camelCase -> snake_case: orderId -> order_id, materialCode -> material_code
    private String toSnake(String s) {
        return s.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    private Object getVal(Map<String, Object> row, String col) {
        if (row.containsKey(col)) return row.get(col);
        String camel = col; // 已经是 snake_case，转为 camelCase 试试
        // order_id -> orderId
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : col.toCharArray()) {
            if (c == '_') { up = true; continue; }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        String camelKey = sb.toString();
        if (!camelKey.equals(col) && row.containsKey(camelKey)) return row.get(camelKey);
        return null;
    }

    private Map<String, Object> save(String table, Map<String, Object> row, String[] cols) {
        Long id = row.get("id") != null ? Long.valueOf(row.get("id").toString()) : null;
        boolean insert = (id == null);
        if (insert) {
            id = nextId();
            row.put("id", id);
        }
        String now = now();
        row.putIfAbsent("created_at", now);
        row.put("updated_at", now);

        if (insert) {
            StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (id,");
            StringBuilder vals = new StringBuilder(" VALUES (?,");
            List<Object> params = new ArrayList<>();
            params.add(id);
            for (String c : cols) {
                Object v = getVal(row, c);
                if (v != null) {
                    sql.append(c).append(",");
                    vals.append("?,");
                    params.add(v);
                }
            }
            sql.append("created_at,updated_at)");
            vals.append("?,?)");
            params.add(now); params.add(now);
            db.update(sql.append(vals).toString(), params.toArray());
        } else {
            StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ");
            List<Object> params = new ArrayList<>();
            for (String c : cols) {
                Object v = getVal(row, c);
                if (v != null) {
                    sql.append(c).append("=?,");
                    params.add(v);
                }
            }
            sql.append("updated_at=? WHERE id=?");
            params.add(now); params.add(id);
            db.update(sql.toString(), params.toArray());
        }
        return row;
    }
}
