package com.example.wms.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据访问层。
 *
 * 约定：
 * 1. 所有 SQL 都用 ? 占位符传参，不做字符串拼接；列名只来自本类内的静态白名单，
 *    不来自调用方，因此拼接列名不构成注入面。
 * 2. 物料名/仓库名/库位名等原先冗余存储在业务表里的字段，改为 JOIN 派生，
 *    并用 AS 别名为原来的列名，保证对外字段结构不变。
 * 3. 主键由 MySQL AUTO_INCREMENT 生成，INSERT 后回填到返回的 Map 中。
 */
@Component
public class DataStorage {

    private final JdbcTemplate db;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper JSON = new ObjectMapper();

    public DataStorage(JdbcTemplate db) {
        this.db = db;
    }

    private String now() {
        return LocalDateTime.now().format(DTF);
    }

    // ===================== 基础数据 =====================

    private static final String MATERIAL_SELECT =
            "SELECT m.*, s.name AS supplier_name FROM materials m "
                    + "LEFT JOIN suppliers s ON s.id = m.supplier_id";

    public List<Map<String, Object>> findAllMaterials() {
        return list(MATERIAL_SELECT + " ORDER BY m.id");
    }

    public Map<String, Object> findMaterialById(Long id) {
        return one(MATERIAL_SELECT + " WHERE m.id = ?", id);
    }

    public Map<String, Object> saveMaterial(Map<String, Object> row) {
        return save("materials", row,
                "code,name,spec,unit,package_qty,category,min_stock,max_stock,supplier_id,price".split(","));
    }

    public void deleteMaterial(Long id) {
        db.update("DELETE FROM materials WHERE id = ?", id);
    }

    public List<Map<String, Object>> findAllSuppliers() {
        return list("SELECT * FROM suppliers ORDER BY id");
    }

    public Map<String, Object> findSupplierById(Long id) {
        return one("SELECT * FROM suppliers WHERE id = ?", id);
    }

    public Map<String, Object> saveSupplier(Map<String, Object> row) {
        return save("suppliers", row, "code,name,contact_name,contact_phone,address,province,city".split(","));
    }

    public void deleteSupplier(Long id) {
        db.update("DELETE FROM suppliers WHERE id = ?", id);
    }

    public List<Map<String, Object>> findAllCustomers() {
        return list("SELECT * FROM customers ORDER BY id");
    }

    public Map<String, Object> findCustomerById(Long id) {
        return one("SELECT * FROM customers WHERE id = ?", id);
    }

    public Map<String, Object> saveCustomer(Map<String, Object> row) {
        return save("customers", row, "code,name,contact_name,contact_phone,address,province,city".split(","));
    }

    public void deleteCustomer(Long id) {
        db.update("DELETE FROM customers WHERE id = ?", id);
    }

    public List<Map<String, Object>> findAllWarehouses() {
        return list("SELECT * FROM warehouses ORDER BY id");
    }

    public Map<String, Object> findWarehouseById(Long id) {
        return one("SELECT * FROM warehouses WHERE id = ?", id);
    }

    public Map<String, Object> saveWarehouse(Map<String, Object> row) {
        return save("warehouses", row, "code,name,address,city,type".split(","));
    }

    public void deleteWarehouse(Long id) {
        db.update("DELETE FROM warehouses WHERE id = ?", id);
    }

    private static final String LOCATION_SELECT =
            "SELECT l.*, w.name AS warehouse_name FROM locations l "
                    + "JOIN warehouses w ON w.id = l.warehouse_id";

    public List<Map<String, Object>> findAllLocations() {
        return list(LOCATION_SELECT + " ORDER BY l.id");
    }

    public Map<String, Object> findLocationById(Long id) {
        return one(LOCATION_SELECT + " WHERE l.id = ?", id);
    }

    public Map<String, Object> saveLocation(Map<String, Object> row) {
        return save("locations", row, "warehouse_id,code,name,area,shelf,layer".split(","));
    }

    public void deleteLocation(Long id) {
        db.update("DELETE FROM locations WHERE id = ?", id);
    }

    // ===================== 入库单 & 明细 =====================

    private static final String ORDER_SELECT =
            "SELECT o.*, o.created_at AS create_time, s.name AS supplier_name, w.name AS warehouse_name "
                    + "FROM inbound_orders o "
                    + "LEFT JOIN suppliers s ON s.id = o.supplier_id "
                    + "LEFT JOIN warehouses w ON w.id = o.warehouse_id";

    public List<Map<String, Object>> findAllOrders() {
        return list(ORDER_SELECT + " ORDER BY o.created_at DESC, o.id DESC");
    }

    public Map<String, Object> findOrderById(Long id) {
        return one(ORDER_SELECT + " WHERE o.id = ?", id);
    }

    public Map<String, Object> saveOrder(Map<String, Object> row) {
        return save("inbound_orders", row,
                "order_no,inbound_type,supplier_id,warehouse_id,status,plan_date,create_by,reverse_reason,remark".split(","));
    }

    public void deleteOrder(Long id) {
        db.update("DELETE FROM inbound_orders WHERE id = ?", id);
    }

    private static final String ITEM_SELECT =
            "SELECT i.*, m.code AS material_code, m.name AS material_name, m.spec AS spec, m.unit AS unit, "
                    + "w.name AS warehouse_name, l.name AS location_name "
                    + "FROM inbound_order_items i "
                    + "JOIN materials m ON m.id = i.material_id "
                    + "LEFT JOIN warehouses w ON w.id = i.warehouse_id "
                    + "LEFT JOIN locations l ON l.id = i.location_id";

    public List<Map<String, Object>> findItemsByOrderId(Long orderId) {
        return list(ITEM_SELECT + " WHERE i.order_id = ? ORDER BY i.id", orderId);
    }

    public Map<String, Object> findItemById(Long id) {
        return one(ITEM_SELECT + " WHERE i.id = ?", id);
    }

    public Map<String, Object> saveItem(Map<String, Object> row) {
        return save("inbound_order_items", row,
                "order_id,material_id,plan_qty,received_qty,batch_no,location_id,warehouse_id,status".split(","));
    }

    public void deleteItem(Long id) {
        db.update("DELETE FROM inbound_order_items WHERE id = ?", id);
    }

    public void deleteItemsByOrderId(Long orderId) {
        db.update("DELETE FROM inbound_order_items WHERE order_id = ?", orderId);
    }

    // ===================== 看板 =====================

    private static final String KANBAN_SELECT =
            "SELECT k.*, k.created_at AS create_time, o.order_no AS order_no, "
                    + "m.code AS material_code, m.name AS material_name, m.spec AS spec, "
                    + "w.name AS warehouse_name, l.name AS location_name "
                    + "FROM kanbans k "
                    + "LEFT JOIN inbound_orders o ON o.id = k.order_id "
                    + "LEFT JOIN materials m ON m.id = k.material_id "
                    + "LEFT JOIN warehouses w ON w.id = k.warehouse_id "
                    + "LEFT JOIN locations l ON l.id = k.location_id";

    public List<Map<String, Object>> findAllKanbans() {
        return list(KANBAN_SELECT + " ORDER BY k.created_at DESC, k.id DESC");
    }

    public Map<String, Object> findKanbanById(Long id) {
        return one(KANBAN_SELECT + " WHERE k.id = ?", id);
    }

    public Map<String, Object> findKanbanByCode(String code) {
        return one(KANBAN_SELECT + " WHERE k.kanban_code = ?", code);
    }

    public Map<String, Object> saveKanban(Map<String, Object> row) {
        return save("kanbans", row,
                ("kanban_code,order_id,order_item_id,material_id,plan_qty,received_qty,warehouse_id,location_id,"
                        + "batch_no,status,print_count,package_info,last_print_time").split(","));
    }

    public void deleteKanban(Long id) {
        db.update("DELETE FROM kanbans WHERE id = ?", id);
    }

    public void deleteKanbansByOrderId(Long orderId) {
        db.update("DELETE FROM kanbans WHERE order_id = ?", orderId);
    }

    // ===================== 库存 =====================

    private static final String INVENTORY_SELECT =
            "SELECT i.*, m.code AS material_code, m.name AS material_name, m.spec AS spec, m.unit AS unit, "
                    + "w.name AS warehouse_name, l.name AS location_name "
                    + "FROM inventories i "
                    + "JOIN materials m ON m.id = i.material_id "
                    + "JOIN warehouses w ON w.id = i.warehouse_id "
                    + "JOIN locations l ON l.id = i.location_id";

    public List<Map<String, Object>> findAllInventories() {
        return list(INVENTORY_SELECT + " ORDER BY i.updated_at DESC, i.id DESC");
    }

    public Map<String, Object> findInventoryById(Long id) {
        return one(INVENTORY_SELECT + " WHERE i.id = ?", id);
    }

    /** 库存业务主键：物料 + 仓库 + 库位 + 批次。 */
    public Map<String, Object> findInventoryByKey(Long materialId, Long warehouseId, Long locationId, String batchNo) {
        return one(INVENTORY_SELECT
                        + " WHERE i.material_id = ? AND i.warehouse_id = ? AND i.location_id = ? AND i.batch_no = ?",
                materialId, warehouseId, locationId, batchNo == null ? "" : batchNo);
    }

    public Map<String, Object> saveInventory(Map<String, Object> row) {
        Object qtyObj = row.get("qty");
        if (qtyObj != null) {
            BigDecimal qty = new BigDecimal(qtyObj.toString());
            row.put("status", qty.compareTo(BigDecimal.ZERO) == 0 ? "已耗尽" : "已入库");
        }
        // batch_no 是库存业务主键的一部分且非空，统一把 null 归一成空串
        if (row.containsKey("batchNo") && row.get("batchNo") == null) row.put("batchNo", "");
        if (row.containsKey("batch_no") && row.get("batch_no") == null) row.put("batch_no", "");
        return save("inventories", row,
                "material_id,warehouse_id,location_id,batch_no,qty,frozen_qty,package_qty,status".split(","));
    }

    public void deleteInventory(Long id) {
        db.update("DELETE FROM inventories WHERE id = ?", id);
    }

    // ===================== 流水记录 =====================

    private static final String RECORD_SELECT =
            "SELECT r.*, m.code AS material_code, m.name AS material_name, "
                    + "w.name AS warehouse_name, l.name AS location_name "
                    + "FROM records r "
                    + "LEFT JOIN materials m ON m.id = r.material_id "
                    + "LEFT JOIN warehouses w ON w.id = r.warehouse_id "
                    + "LEFT JOIN locations l ON l.id = r.location_id";

    public List<Map<String, Object>> findAllRecords() {
        return list(RECORD_SELECT + " ORDER BY r.operate_time DESC, r.id DESC");
    }

    public Map<String, Object> saveRecord(Map<String, Object> row) {
        return save("records", row,
                ("record_no,business_type,business_no,kanban_code,material_id,warehouse_id,location_id,"
                        + "batch_no,change_qty,before_qty,after_qty,operator,remark,operate_time").split(","));
    }

    // ===================== 出库单 & 明细 =====================

    private static final String OUTBOUND_SELECT =
            "SELECT o.*, o.created_at AS create_time, c.name AS customer_name, w.name AS warehouse_name "
                    + "FROM outbound_orders o "
                    + "LEFT JOIN customers c ON c.id = o.customer_id "
                    + "LEFT JOIN warehouses w ON w.id = o.warehouse_id";

    public List<Map<String, Object>> findAllOutboundOrders() {
        return list(OUTBOUND_SELECT + " ORDER BY o.created_at DESC, o.id DESC");
    }

    public Map<String, Object> findOutboundOrderById(Long id) {
        return one(OUTBOUND_SELECT + " WHERE o.id = ?", id);
    }

    public Map<String, Object> saveOutboundOrder(Map<String, Object> row) {
        return save("outbound_orders", row,
                "order_no,outbound_type,customer_id,warehouse_id,status,plan_date,create_by,reverse_reason,remark".split(","));
    }

    public void deleteOutboundOrder(Long id) {
        db.update("DELETE FROM outbound_orders WHERE id = ?", id);
    }

    private static final String OUTBOUND_ITEM_SELECT =
            "SELECT i.*, m.code AS material_code, m.name AS material_name, m.spec AS spec, m.unit AS unit "
                    + "FROM outbound_order_items i "
                    + "JOIN materials m ON m.id = i.material_id";

    public List<Map<String, Object>> findAllOutboundItems() {
        return list(OUTBOUND_ITEM_SELECT + " ORDER BY i.id");
    }

    public List<Map<String, Object>> findOutboundItemsByOrderId(Long orderId) {
        return list(OUTBOUND_ITEM_SELECT + " WHERE i.order_id = ? ORDER BY i.id", orderId);
    }

    public Map<String, Object> findOutboundItemById(Long id) {
        return one(OUTBOUND_ITEM_SELECT + " WHERE i.id = ?", id);
    }

    public Map<String, Object> saveOutboundItem(Map<String, Object> row) {
        return save("outbound_order_items", row,
                "order_id,material_id,plan_qty,shipped_qty,batch_no,status".split(","));
    }

    public void deleteOutboundItem(Long id) {
        db.update("DELETE FROM outbound_order_items WHERE id = ?", id);
    }

    public void deleteOutboundItemsByOrderId(Long orderId) {
        db.update("DELETE FROM outbound_order_items WHERE order_id = ?", orderId);
    }

    // ===================== 封存 / 解封 / 转包 =====================

    private static final String FREEZE_SELECT =
            "SELECT f.*, m.code AS material_code, m.name AS material_name, "
                    + "w.name AS warehouse_name, l.name AS location_name "
                    + "FROM freeze_records f "
                    + "LEFT JOIN materials m ON m.id = f.material_id "
                    + "LEFT JOIN warehouses w ON w.id = f.warehouse_id "
                    + "LEFT JOIN locations l ON l.id = f.location_id";

    public List<Map<String, Object>> findAllFreezeRecords() {
        return list(FREEZE_SELECT + " ORDER BY f.created_at DESC, f.id DESC");
    }

    public Map<String, Object> findFreezeRecordById(Long id) {
        return one(FREEZE_SELECT + " WHERE f.id = ?", id);
    }

    public Map<String, Object> saveFreezeRecord(Map<String, Object> row) {
        return save("freeze_records", row,
                ("freeze_no,inventory_id,material_id,warehouse_id,location_id,batch_no,freeze_qty,"
                        + "before_frozen_qty,after_frozen_qty,unfrozen_qty,freeze_reason,operator,status").split(","));
    }

    public List<Map<String, Object>> findAllUnfreezeRecords() {
        return list("SELECT * FROM unfreeze_records ORDER BY created_at DESC, id DESC");
    }

    public List<Map<String, Object>> findUnfreezeRecordsByFreezeId(Long freezeId) {
        return list("SELECT * FROM unfreeze_records WHERE freeze_id = ? ORDER BY created_at DESC, id DESC", freezeId);
    }

    public Map<String, Object> saveUnfreezeRecord(Map<String, Object> row) {
        return save("unfreeze_records", row, "freeze_id,unfreeze_qty,unfreeze_reason,operator".split(","));
    }

    private static final String REPACK_SELECT =
            "SELECT p.*, m.code AS material_code, m.name AS material_name, "
                    + "sw.name AS from_warehouse_name, sl.name AS from_location_name, "
                    + "tw.name AS to_warehouse_name, tl.name AS to_location_name "
                    + "FROM repack_records p "
                    + "LEFT JOIN materials m ON m.id = p.material_id "
                    + "LEFT JOIN inventories si ON si.id = p.source_inventory_id "
                    + "LEFT JOIN warehouses sw ON sw.id = si.warehouse_id "
                    + "LEFT JOIN locations sl ON sl.id = si.location_id "
                    + "LEFT JOIN warehouses tw ON tw.id = p.to_warehouse_id "
                    + "LEFT JOIN locations tl ON tl.id = p.to_location_id";

    public List<Map<String, Object>> findAllRepackRecords() {
        return list(REPACK_SELECT + " ORDER BY p.created_at DESC, p.id DESC");
    }

    public Map<String, Object> saveRepackRecord(Map<String, Object> row) {
        return save("repack_records", row,
                ("repack_no,source_inventory_id,target_inventory_id,material_id,to_warehouse_id,to_location_id,"
                        + "from_batch_no,to_batch_no,repack_qty,before_qty,after_qty,package_qty,operator,reason").split(","));
    }

    public List<Map<String, Object>> findAllInboundRecords() {
        return list(RECORD_SELECT + " WHERE r.business_type = ? ORDER BY r.operate_time DESC, r.id DESC", "入库");
    }

    public List<Map<String, Object>> findInboundRecordsByOrderId(Long orderId) {
        return list(RECORD_SELECT
                        + " WHERE r.business_no = (SELECT o.order_no FROM inbound_orders o WHERE o.id = ?)"
                        + " ORDER BY r.operate_time DESC, r.id DESC",
                orderId);
    }

    public Map<String, Object> saveInboundRecord(Map<String, Object> row) {
        return saveRecord(row);
    }

    // ===================== 预警 =====================

    private static final String ALERT_SELECT =
            "SELECT a.*, m.code AS material_code, m.name AS material_name, "
                    + "w.name AS warehouse_name, l.name AS location_name "
                    + "FROM alert_logs a "
                    + "LEFT JOIN materials m ON m.id = a.material_id "
                    + "LEFT JOIN warehouses w ON w.id = a.warehouse_id "
                    + "LEFT JOIN locations l ON l.id = a.location_id";

    public List<Map<String, Object>> findAllAlertLogs() {
        return list(ALERT_SELECT + " ORDER BY a.created_at DESC, a.id DESC");
    }

    public Map<String, Object> findAlertLogById(Long id) {
        return one(ALERT_SELECT + " WHERE a.id = ?", id);
    }

    /** 查找同类型、同物料、同批次且未处理的预警，用于去重。 */
    public Map<String, Object> findOpenAlert(String alertType, Long materialId, String batchNo) {
        return one(ALERT_SELECT
                        + " WHERE a.alert_type = ? AND a.material_id = ? AND COALESCE(a.batch_no, '') = ?"
                        + " AND a.status = '待处理' LIMIT 1",
                alertType, materialId, batchNo == null ? "" : batchNo);
    }

    public Map<String, Object> saveAlertLog(Map<String, Object> row) {
        return save("alert_logs", row,
                ("alert_no,alert_type,level,status,material_id,warehouse_id,location_id,batch_no,qty,available_qty,"
                        + "threshold_qty,daily_avg_out,lead_time_days,stagnant_days,message,suggestion,handler,action,"
                        + "remark,handled_at").split(","));
    }

    private static final String ALERT_CONFIG_SELECT =
            "SELECT c.*, m.code AS material_code, m.name AS material_name "
                    + "FROM alert_configs c "
                    + "LEFT JOIN materials m ON m.id = c.material_id";

    public List<Map<String, Object>> findAllAlertConfigs() {
        return list(ALERT_CONFIG_SELECT + " ORDER BY c.id");
    }

    public Map<String, Object> findAlertConfigById(Long id) {
        return one(ALERT_CONFIG_SELECT + " WHERE c.id = ?", id);
    }

    public Map<String, Object> findAlertConfigByMaterialId(Long materialId) {
        return one(ALERT_CONFIG_SELECT + " WHERE c.material_id = ?", materialId);
    }

    public Map<String, Object> saveAlertConfig(Map<String, Object> row) {
        return save("alert_configs", row, "material_id,lead_time_days,stagnant_days,enabled".split(","));
    }

    public void deleteAlertConfig(Long id) {
        db.update("DELETE FROM alert_configs WHERE id = ?", id);
    }

    // ===================== 出库策略 / PDA 离线缓存 =====================

    public List<Map<String, Object>> findAllFifoConfigs() {
        return list("SELECT * FROM fifo_configs ORDER BY id");
    }

    public Map<String, Object> findFifoConfigById(Long id) {
        return one("SELECT * FROM fifo_configs WHERE id = ?", id);
    }

    public Map<String, Object> saveFifoConfig(Map<String, Object> row) {
        return save("fifo_configs", row, "mode,active".split(","));
    }

    public List<Map<String, Object>> findAllPdaCaches() {
        return list("SELECT * FROM pda_caches ORDER BY created_at DESC, id DESC");
    }

    public Map<String, Object> savePdaCache(Map<String, Object> row) {
        // data 是 Map，落库前序列化成 JSON 文本（前端不读该字段，仅作离线补传载荷）
        Object data = row.get("data");
        if (data != null && !(data instanceof String)) {
            try {
                row.put("data", JSON.writeValueAsString(data));
            } catch (Exception e) {
                row.put("data", String.valueOf(data));
            }
        }
        return save("pda_caches", row, "device_id,op_type,data,sync_status".split(","));
    }

    public void deletePdaCache(Long id) {
        db.update("DELETE FROM pda_caches WHERE id = ?", id);
    }

    // ===================== 用户 =====================

    public List<Map<String, Object>> findAllUsers() {
        return list("SELECT * FROM users ORDER BY id");
    }

    public Map<String, Object> findUserByUsername(String username) {
        return one("SELECT * FROM users WHERE username = ?", username);
    }

    public Map<String, Object> saveUser(Map<String, Object> row) {
        return save("users", row, "username,password,nickname,role".split(","));
    }

    public String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000);
    }

    // ===================== 数据库操作方法 =====================

    private List<Map<String, Object>> list(String sql, Object... args) {
        return db.query(sql, (rs, i) -> rowFromRs(rs), args);
    }

    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = list(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 结果行转 Map，同时提供下划线与驼峰两种键名。 */
    private Map<String, Object> rowFromRs(ResultSet rs) throws SQLException {
        Map<String, Object> row = new Row();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    /**
     * 双键名行对象：写入任一形式的键都会同步更新另一个。
     *
     * 为什么需要它：服务层读出记录后统一用驼峰键改写（如 receivedQty），
     * 而 save() 的白名单用的是下划线列名（received_qty）。若两个键各自独立，
     * 保存时读到的就是未更新的旧值，数量字段会静默写不进去。
     */
    static final class Row extends LinkedHashMap<String, Object> {
        @Override
        public Object put(String key, Object value) {
            Object prev = super.put(key, value);
            String alias = aliasOf(key);
            if (alias != null) super.put(alias, value);
            return prev;
        }

        private static String aliasOf(String key) {
            return key.indexOf('_') >= 0 ? toCamel(key) : toSnake(key);
        }
    }

    /** received_qty -> receivedQty */
    private static String toCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                up = true;
                continue;
            }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    /** receivedQty -> received_qty；已是下划线形式或没有大写字母时返回 null。 */
    private static String toSnake(String s) {
        StringBuilder sb = new StringBuilder();
        boolean changed = false;
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
                changed = true;
            } else {
                sb.append(c);
            }
        }
        return changed ? sb.toString() : null;
    }

    /** 从行里取列值，兼容下划线与驼峰两种键名。 */
    private Object getVal(Map<String, Object> row, String col) {
        if (row.containsKey(col)) return row.get(col);
        String camelKey = toCamel(col);
        return camelKey.equals(col) ? null : row.get(camelKey);
    }

    /**
     * 通用写入。row 带 id 走 UPDATE，否则走 INSERT 并由数据库分配主键。
     *
     * 列名来自调用方传入的静态白名单（服务层不会把用户输入拼进来），
     * 值一律用 ? 占位，因此不存在注入面。
     */
    private Map<String, Object> save(String table, Map<String, Object> row, String[] cols) {
        Object idVal = getVal(row, "id");
        boolean insert = (idVal == null);

        if (insert) {
            StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");
            StringBuilder marks = new StringBuilder(" VALUES (");
            List<Object> params = new ArrayList<>();
            List<String> usedCols = new ArrayList<>();
            for (String c : cols) {
                Object v = getVal(row, c);
                if (v != null) {
                    usedCols.add(c);
                    params.add(v);
                }
            }
            if (usedCols.isEmpty()) throw new IllegalArgumentException(table + " 插入时没有任何可写字段");
            sql.append(String.join(",", usedCols)).append(")");
            marks.append(String.join(",", usedCols.stream().map(c -> "?").toArray(String[]::new))).append(")");
            sql.append(marks);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            db.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                row.put("id", key.longValue());
            }
        } else {
            Long id = Long.valueOf(idVal.toString());
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
            params.add(now());
            params.add(id);
            db.update(sql.toString(), params.toArray());
        }
        return row;
    }
}
