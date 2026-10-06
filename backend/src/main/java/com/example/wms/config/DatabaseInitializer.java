package com.example.wms.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate db;

    public DatabaseInitializer(JdbcTemplate db) { this.db = db; }

    @Override
    public void run(String... args) {
        createTables();
        if (count("materials") == 0) seedMaterials();
        if (count("suppliers") == 0) seedSuppliers();
        if (count("customers") == 0) seedCustomers();
        if (count("warehouses") == 0) seedWarehouses();
        if (count("locations") == 0) seedLocations();
        if (count("users") == 0) seedUsers();
    }

    private int count(String table) {
        Integer c = db.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return c == null ? 0 : c;
    }

    private void createTables() {
        db.execute("""
            CREATE TABLE IF NOT EXISTS materials (
                id BIGINT PRIMARY KEY, code VARCHAR(50) NOT NULL, name VARCHAR(100) NOT NULL,
                spec VARCHAR(100), unit VARCHAR(20), package_qty INT DEFAULT 0,
                category VARCHAR(50), min_stock INT DEFAULT 0, max_stock INT DEFAULT 9999,
                supplier_id BIGINT, price DECIMAL(12,2) DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_material_code (code), INDEX idx_material_name (name), INDEX idx_material_cat (category)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS suppliers (
                id BIGINT PRIMARY KEY, code VARCHAR(50) NOT NULL, name VARCHAR(150) NOT NULL,
                contact_name VARCHAR(50), contact_phone VARCHAR(30), address VARCHAR(200),
                province VARCHAR(50), city VARCHAR(50),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS customers (
                id BIGINT PRIMARY KEY, code VARCHAR(50) NOT NULL, name VARCHAR(150) NOT NULL,
                contact_name VARCHAR(50), contact_phone VARCHAR(30), address VARCHAR(200),
                province VARCHAR(50), city VARCHAR(50),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS warehouses (
                id BIGINT PRIMARY KEY, code VARCHAR(50) NOT NULL, name VARCHAR(150) NOT NULL,
                address VARCHAR(200), city VARCHAR(50), type VARCHAR(30),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS locations (
                id BIGINT PRIMARY KEY, warehouse_id BIGINT NOT NULL,
                code VARCHAR(50) NOT NULL, name VARCHAR(100) NOT NULL,
                area VARCHAR(20), shelf VARCHAR(20), layer VARCHAR(20),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_loc_wh (warehouse_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS inbound_orders (
                id BIGINT PRIMARY KEY, order_no VARCHAR(50) NOT NULL UNIQUE,
                supplier_id BIGINT, warehouse_id BIGINT, status VARCHAR(20) DEFAULT '待审核',
                expected_date DATE, remark VARCHAR(500),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS inbound_order_items (
                id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, material_id BIGINT NOT NULL,
                plan_qty DECIMAL(12,2) DEFAULT 0, received_qty DECIMAL(12,2) DEFAULT 0,
                batch_no VARCHAR(100), location_id BIGINT, warehouse_id BIGINT,
                INDEX idx_ioi_order (order_id), INDEX idx_ioi_mat (material_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS kanbans (
                id BIGINT PRIMARY KEY, kanban_code VARCHAR(50) NOT NULL UNIQUE,
                order_id BIGINT, order_item_id BIGINT, material_id BIGINT,
                order_no VARCHAR(50), material_code VARCHAR(50), material_name VARCHAR(100),
                plan_qty DECIMAL(12,2), received_qty DECIMAL(12,2) DEFAULT 0,
                warehouse_id BIGINT, warehouse_name VARCHAR(100),
                location_id BIGINT, location_name VARCHAR(100),
                batch_no VARCHAR(100), status VARCHAR(20) DEFAULT '未打印',
                print_count INT DEFAULT 0, package_info VARCHAR(50),
                last_print_time DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_kb_code (kanban_code), INDEX idx_kb_order (order_id), INDEX idx_kb_mat (material_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS inventories (
                id BIGINT PRIMARY KEY, material_id BIGINT NOT NULL,
                warehouse_id BIGINT, location_id BIGINT,
                material_code VARCHAR(50), material_name VARCHAR(100),
                warehouse_name VARCHAR(100), location_name VARCHAR(100),
                batch_no VARCHAR(100), qty DECIMAL(12,2) DEFAULT 0,
                frozen_qty DECIMAL(12,2) DEFAULT 0, package_qty DECIMAL(12,2) DEFAULT 0,
                unit VARCHAR(20), status VARCHAR(20) DEFAULT '已入库',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_inv_mat (material_id), INDEX idx_inv_wh (warehouse_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS records (
                id BIGINT PRIMARY KEY, record_no VARCHAR(50) NOT NULL,
                business_type VARCHAR(20), business_no VARCHAR(50),
                kanban_code VARCHAR(50), material_id BIGINT,
                material_code VARCHAR(50), material_name VARCHAR(100),
                warehouse_id BIGINT, warehouse_name VARCHAR(100),
                location_id BIGINT, location_name VARCHAR(100),
                batch_no VARCHAR(100), change_qty DECIMAL(12,2),
                before_qty DECIMAL(12,2), after_qty DECIMAL(12,2),
                operator VARCHAR(50), remark VARCHAR(500),
                operate_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_rec_kanban (kanban_code), INDEX idx_rec_mat (material_id), INDEX idx_rec_time (operate_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS outbound_orders (
                id BIGINT PRIMARY KEY, order_no VARCHAR(50) NOT NULL UNIQUE,
                customer_id BIGINT, warehouse_id BIGINT, status VARCHAR(20) DEFAULT '待发货',
                plan_date DATE, remark VARCHAR(500),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS outbound_order_items (
                id BIGINT PRIMARY KEY, order_id BIGINT NOT NULL, material_id BIGINT NOT NULL,
                plan_qty DECIMAL(12,2) DEFAULT 0, shipped_qty DECIMAL(12,2) DEFAULT 0,
                batch_no VARCHAR(100),
                INDEX idx_ooi_order (order_id), INDEX idx_ooi_mat (material_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(100), nickname VARCHAR(50), role VARCHAR(30) DEFAULT 'operator',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        // 辅助表
        db.execute("""
            CREATE TABLE IF NOT EXISTS freeze_records (
                id BIGINT PRIMARY KEY, freeze_no VARCHAR(50), inventory_id BIGINT,
                material_id BIGINT, warehouse_id BIGINT, warehouse_name VARCHAR(100),
                location_id BIGINT, location_name VARCHAR(100),
                material_code VARCHAR(50), material_name VARCHAR(100),
                batch_no VARCHAR(100), qty DECIMAL(12,2), reason VARCHAR(300),
                freeze_qty DECIMAL(12,2), before_frozen_qty DECIMAL(12,2), after_frozen_qty DECIMAL(12,2),
                unfrozen_qty DECIMAL(12,2) DEFAULT 0, freeze_reason VARCHAR(300),
                operator VARCHAR(50), status VARCHAR(20) DEFAULT '已冻结',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS unfreeze_records (
                id BIGINT PRIMARY KEY, freeze_id BIGINT NOT NULL,
                unfreeze_qty DECIMAL(12,2), unfreeze_reason VARCHAR(300),
                operator VARCHAR(50),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_unfreeze_freeze (freeze_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS repack_records (
                id BIGINT PRIMARY KEY, repack_no VARCHAR(50), source_inventory_id BIGINT, target_inventory_id BIGINT,
                material_id BIGINT, material_code VARCHAR(50), material_name VARCHAR(100),
                from_warehouse_name VARCHAR(100), from_location_name VARCHAR(100),
                to_warehouse_id BIGINT, to_warehouse_name VARCHAR(100),
                to_location_id BIGINT, to_location_name VARCHAR(100),
                from_batch_no VARCHAR(100), to_batch_no VARCHAR(100),
                repack_qty DECIMAL(12,2), before_qty DECIMAL(12,2), after_qty DECIMAL(12,2),
                package_qty DECIMAL(12,2), operator VARCHAR(50), reason VARCHAR(300),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_repack_mat (material_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS alert_logs (
                id BIGINT PRIMARY KEY, alert_no VARCHAR(50), alert_type VARCHAR(30), level VARCHAR(20),
                status VARCHAR(20) DEFAULT '待处理', material_id BIGINT, material_code VARCHAR(50), material_name VARCHAR(100),
                warehouse_name VARCHAR(100), location_name VARCHAR(100), batch_no VARCHAR(100),
                qty DECIMAL(12,2), available_qty DECIMAL(12,2), threshold_qty DECIMAL(12,2),
                daily_avg_out DECIMAL(12,2), lead_time_days INT, stagnant_days INT,
                message VARCHAR(500), suggestion VARCHAR(500),
                handler VARCHAR(50), action VARCHAR(100), remark VARCHAR(500), handled_at DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_alert_status (status), INDEX idx_alert_type (alert_type), INDEX idx_alert_mat (material_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS alert_configs (
                id BIGINT PRIMARY KEY, material_id BIGINT,
                lead_time_days INT DEFAULT 7, stagnant_days INT DEFAULT 180,
                enabled TINYINT DEFAULT 1,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_alert_cfg_mat (material_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        db.execute("""
            CREATE TABLE IF NOT EXISTS allocations (
                id BIGINT PRIMARY KEY, order_id BIGINT, order_item_id BIGINT,
                inventory_id BIGINT, material_code VARCHAR(50), material_name VARCHAR(100),
                batch_no VARCHAR(100), alloc_qty DECIMAL(12,2),
                warehouse_name VARCHAR(100), location_name VARCHAR(100),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
        // 确保所有表都有 created_at / updated_at 列
        String[] tables = {"materials","suppliers","customers","warehouses","locations",
            "inbound_orders","inbound_order_items","kanbans","inventories","records",
            "outbound_orders","outbound_order_items","users","freeze_records","unfreeze_records",
            "repack_records","alert_logs","alert_configs","allocations"};
        for (String t : tables) {
            try { db.execute("ALTER TABLE " + t + " ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
            try { db.execute("ALTER TABLE " + t + " ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"); } catch (Exception ignored) {}
        }
        // 补齐 inbound_order_items 缺失的业务字段
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN material_code VARCHAR(50)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN material_name VARCHAR(100)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN spec VARCHAR(100)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN unit VARCHAR(20)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN warehouse_name VARCHAR(100)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN location_name VARCHAR(100)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inbound_order_items ADD COLUMN status VARCHAR(20)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inventories ADD COLUMN frozen_qty DECIMAL(12,2) DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE inventories ADD COLUMN package_qty DECIMAL(12,2) DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN material_id BIGINT"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN warehouse_id BIGINT"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN warehouse_name VARCHAR(100)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN location_id BIGINT"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN location_name VARCHAR(100)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN freeze_qty DECIMAL(12,2)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN before_frozen_qty DECIMAL(12,2)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN after_frozen_qty DECIMAL(12,2)"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN unfrozen_qty DECIMAL(12,2) DEFAULT 0"); } catch (Exception ignored) {}
        try { db.execute("ALTER TABLE freeze_records ADD COLUMN freeze_reason VARCHAR(300)"); } catch (Exception ignored) {}
        System.out.println("=== 数据库表创建完成 ===");
    }

    // ===================== 物料种子数据 100+ 种 =====================
    private void seedMaterials() {
        Object[][] mats = {
            // ===== 发动机系统 (EA888) =====
            {2001L, "EA888-001", "活塞总成", "Φ82.5mm EA888 Gen3", "套", 4, "发动机系统"},
            {2002L, "EA888-002", "活塞环组", "Φ82.5mm 三道环", "组", 10, "发动机系统"},
            {2003L, "EA888-003", "连杆总成", "EA888 锻造", "根", 8, "发动机系统"},
            {2004L, "EA888-004", "曲轴总成", "EA888 锻钢", "根", 2, "发动机系统"},
            {2005L, "EA888-005", "凸轮轴进气", "EA888 Gen3 进气", "根", 4, "发动机系统"},
            {2006L, "EA888-006", "凸轮轴排气", "EA888 Gen3 排气", "根", 4, "发动机系统"},
            {2007L, "EA888-007", "进气门", "Φ34mm EA888", "支", 16, "发动机系统"},
            {2008L, "EA888-008", "排气门", "Φ28mm EA888", "支", 16, "发动机系统"},
            {2009L, "EA888-009", "气门弹簧", "EA888 专用", "个", 32, "发动机系统"},
            {2010L, "EA888-010", "气缸垫", "EA888 三层金属", "张", 4, "发动机系统"},
            {2011L, "EA888-011", "正时链条", "EA888 正时链", "根", 4, "发动机系统"},
            {2012L, "EA888-012", "正时链条张紧器", "EA888 液压式", "个", 4, "发动机系统"},
            {2013L, "EA888-013", "机油泵总成", "EA888 可变排量", "台", 2, "发动机系统"},
            {2014L, "EA888-014", "水泵总成", "EA888 电子水泵", "台", 2, "发动机系统"},
            {2015L, "EA888-015", "油底壳", "EA888 铝合金", "个", 2, "发动机系统"},
            {2016L, "EA888-016", "气缸盖总成", "EA888 铝合金", "台", 1, "发动机系统"},
            {2017L, "EA888-017", "进气歧管", "EA888 可变进气", "个", 2, "发动机系统"},
            {2018L, "EA888-018", "排气歧管", "EA888 不锈钢", "个", 2, "发动机系统"},
            {2019L, "EA888-019", "涡轮增压器总成", "EA888 IHI涡轮", "台", 1, "发动机系统"},
            {2020L, "EA888-020", "中冷器", "EA888 风冷式", "个", 1, "发动机系统"},
            {2021L, "EA888-021", "高压油泵", "EA888 直喷", "个", 2, "发动机系统"},
            {2022L, "EA888-022", "喷油嘴", "EA888 6孔直喷", "个", 8, "发动机系统"},
            {2023L, "EA888-023", "点火线圈", "EA888 独立点火", "个", 8, "发动机系统"},
            {2024L, "EA888-024", "火花塞", "EA888 铱金", "个", 16, "发动机系统"},
            {2025L, "EA888-025", "发动机控制单元ECU", "EA888 Bosch MED17", "台", 1, "发动机系统"},
            {2026L, "EA888-026", "机油滤清器", "EA888 专用", "个", 8, "发动机系统"},

            // ===== 变速箱系统 (DQ381) =====
            {3001L, "DQ381-001", "离合器片组", "DQ381 湿式多片", "套", 3, "变速箱系统"},
            {3002L, "DQ381-002", "双质量飞轮", "DQ381 专用", "个", 2, "变速箱系统"},
            {3003L, "DQ381-003", "输入轴", "DQ381 第一轴", "根", 2, "变速箱系统"},
            {3004L, "DQ381-004", "输出轴", "DQ381 第二轴", "根", 2, "变速箱系统"},
            {3005L, "DQ381-005", "同步器总成1/2档", "DQ381 双锥面", "套", 2, "变速箱系统"},
            {3006L, "DQ381-006", "同步器总成3/4档", "DQ381 单锥面", "套", 2, "变速箱系统"},
            {3007L, "DQ381-007", "换挡拨叉", "DQ381 电控液压", "个", 4, "变速箱系统"},
            {3008L, "DQ381-008", "变速箱壳体", "DQ381 铝合金", "个", 1, "变速箱系统"},
            {3009L, "DQ381-009", "差速器总成", "DQ381 前驱", "台", 1, "变速箱系统"},
            {3010L, "DQ381-010", "半轴总成左", "DQ381 前左", "根", 2, "变速箱系统"},
            {3011L, "DQ381-011", "半轴总成右", "DQ381 前右", "根", 2, "变速箱系统"},
            {3012L, "DQ381-012", "变速箱油泵", "DQ381 电动", "台", 2, "变速箱系统"},
            {3013L, "DQ381-013", "液力变矩器", "DQ381 专用", "台", 1, "变速箱系统"},
            {3014L, "DQ381-014", "变速箱阀体", "DQ381 电液控制", "个", 2, "变速箱系统"},
            {3015L, "DQ381-015", "变速箱滤清器", "DQ381 专用", "个", 5, "变速箱系统"},
            {3016L, "DQ381-016", "变速箱油底壳", "DQ381 塑料", "个", 3, "变速箱系统"},

            // ===== 底盘系统 (MQB) =====
            {4001L, "MQB-001", "前减震器总成", "MQB 麦弗逊", "根", 4, "底盘系统"},
            {4002L, "MQB-002", "后减震器总成", "MQB 多连杆", "根", 4, "底盘系统"},
            {4003L, "MQB-003", "前螺旋弹簧", "MQB 标准型", "根", 4, "底盘系统"},
            {4004L, "MQB-004", "后螺旋弹簧", "MQB 标准型", "根", 4, "底盘系统"},
            {4005L, "MQB-005", "前下摆臂总成", "MQB 冲压焊接", "个", 4, "底盘系统"},
            {4006L, "MQB-006", "前上摆臂总成", "MQB 铝合金", "个", 4, "底盘系统"},
            {4007L, "MQB-007", "前横向稳定杆", "MQB Φ24mm", "根", 2, "底盘系统"},
            {4008L, "MQB-008", "后横向稳定杆", "MQB Φ20mm", "根", 2, "底盘系统"},
            {4009L, "MQB-009", "转向节", "MQB 球墨铸铁", "个", 4, "底盘系统"},
            {4010L, "MQB-010", "前轮毂轴承", "MQB 三代轮毂", "个", 4, "底盘系统"},
            {4011L, "MQB-011", "副车架总成", "MQB 冲压钢板", "台", 1, "底盘系统"},
            {4012L, "MQB-012", "后桥总成", "MQB 多连杆", "台", 1, "底盘系统"},
            {4013L, "MQB-013", "后控制臂上", "MQB 铝合金", "个", 4, "底盘系统"},
            {4014L, "MQB-014", "后控制臂下", "MQB 钢板冲压", "个", 4, "底盘系统"},

            // ===== 制动系统 =====
            {5001L, "BRAKE-001", "前刹车盘", "Φ312mm 通风盘", "个", 4, "制动系统"},
            {5002L, "BRAKE-002", "后刹车盘", "Φ282mm 实心盘", "个", 4, "制动系统"},
            {5003L, "BRAKE-003", "前刹车片套件", "陶瓷配方", "套", 8, "制动系统"},
            {5004L, "BRAKE-004", "后刹车片套件", "低金属配方", "套", 8, "制动系统"},
            {5005L, "BRAKE-005", "制动总泵", "MQB 真空助力", "台", 2, "制动系统"},
            {5006L, "BRAKE-006", "制动分泵前左", "MQB 前制动钳", "个", 2, "制动系统"},
            {5007L, "BRAKE-007", "ABS/ESC泵总成", "Bosch 9.1 ESP", "台", 1, "制动系统"},
            {5008L, "BRAKE-008", "制动液壶", "MQB 专用", "个", 3, "制动系统"},
            {5009L, "BRAKE-009", "制动软管前", "加强橡胶", "根", 4, "制动系统"},
            {5010L, "BRAKE-010", "电子手刹执行器", "MQB EPB", "个", 2, "制动系统"},

            // ===== 转向系统 =====
            {6001L, "STEER-001", "电动转向机总成", "MQB EPS", "台", 1, "转向系统"},
            {6002L, "STEER-002", "转向横拉杆", "MQB 可调式", "根", 4, "转向系统"},
            {6003L, "STEER-003", "转向外球头", "MQB 免维护", "个", 4, "转向系统"},
            {6004L, "STEER-004", "转向内球头", "MQB 专用", "个", 4, "转向系统"},
            {6005L, "STEER-005", "转向中间轴", "MQB 万向节式", "根", 2, "转向系统"},
            {6006L, "STEER-006", "方向盘总成", "MQB 多功能", "个", 1, "转向系统"},

            // ===== 冷却系统 =====
            {7001L, "COOL-001", "散热器总成", "MQB 铝塑", "台", 2, "冷却系统"},
            {7002L, "COOL-002", "冷却风扇总成", "MQB 双风扇", "套", 2, "冷却系统"},
            {7003L, "COOL-003", "节温器总成", "EA888 电子节温器", "个", 3, "冷却系统"},
            {7004L, "COOL-004", "冷却液膨胀壶", "MQB 专用", "个", 3, "冷却系统"},
            {7005L, "COOL-005", "冷却液管路套件", "MQB 硅胶管", "套", 3, "冷却系统"},
            {7006L, "COOL-006", "机油冷却器", "EA888 板式", "个", 2, "冷却系统"},

            // ===== 燃油系统 =====
            {8001L, "FUEL-001", "燃油泵总成", "MQB 内置式", "台", 2, "燃油系统"},
            {8002L, "FUEL-002", "燃油滤清器", "MQB 高压", "个", 8, "燃油系统"},
            {8003L, "FUEL-003", "碳罐总成", "MQB 活性炭", "个", 2, "燃油系统"},
            {8004L, "FUEL-004", "燃油箱总成", "MQB 55L塑料", "个", 1, "燃油系统"},
            {8005L, "FUEL-005", "燃油压力调节阀", "EA888 高压", "个", 2, "燃油系统"},

            // ===== 排气系统 =====
            {9001L, "EXH-001", "三元催化转化器", "MQB 国六b", "台", 1, "排气系统"},
            {9002L, "EXH-002", "前消声器总成", "MQB 不锈钢", "个", 2, "排气系统"},
            {9003L, "EXH-003", "后消声器总成", "MQB 双出", "个", 2, "排气系统"},
            {9004L, "EXH-004", "前排气管", "MQB 不锈钢", "根", 2, "排气系统"},
            {9005L, "EXH-005", "氧传感器前", "Bosch LSU 4.9", "个", 4, "排气系统"},
            {9006L, "EXH-006", "氧传感器后", "Bosch LSF 4.2", "个", 4, "排气系统"},

            // ===== 空调系统 =====
            {1101L, "HVAC-001", "空调压缩机", "MQB 变排量", "台", 1, "空调系统"},
            {1102L, "HVAC-002", "冷凝器总成", "MQB 平行流", "台", 2, "空调系统"},
            {1103L, "HVAC-003", "蒸发器总成", "MQB 层叠式", "台", 2, "空调系统"},
            {1104L, "HVAC-004", "膨胀阀", "MQB 热力式", "个", 3, "空调系统"},
            {1105L, "HVAC-005", "鼓风机总成", "MQB 无刷电机", "台", 2, "空调系统"},
            {1106L, "HVAC-006", "空调滤芯", "MQB PM2.5", "个", 10, "空调系统"},

            // ===== 车身外饰 =====
            {1201L, "BODY-001", "前保险杠总成", "MQB 注塑", "个", 2, "车身外饰"},
            {1202L, "BODY-002", "后保险杠总成", "MQB 注塑", "个", 2, "车身外饰"},
            {1203L, "BODY-003", "前翼子板左", "MQB 钢板冲压", "个", 2, "车身外饰"},
            {1204L, "BODY-004", "前翼子板右", "MQB 钢板冲压", "个", 2, "车身外饰"},
            {1205L, "BODY-005", "发动机盖", "MQB 铝合金", "个", 1, "车身外饰"},
            {1206L, "BODY-006", "行李箱盖", "MQB 钢板冲压", "个", 1, "车身外饰"},
            {1207L, "BODY-007", "前车门总成左", "MQB 焊接总成", "个", 1, "车身外饰"},
            {1208L, "BODY-008", "前车门总成右", "MQB 焊接总成", "个", 1, "车身外饰"},
            {1209L, "BODY-009", "后车门总成左", "MQB 焊接总成", "个", 1, "车身外饰"},
            {1210L, "BODY-010", "后车门总成右", "MQB 焊接总成", "个", 1, "车身外饰"},
            {1211L, "BODY-011", "前挡风玻璃", "MQB 夹层玻璃", "块", 3, "车身外饰"},
            {1212L, "BODY-012", "后挡风玻璃", "MQB 钢化玻璃", "块", 3, "车身外饰"},
            {1213L, "BODY-013", "外后视镜总成左", "MQB 电动折叠加热", "个", 2, "车身外饰"},
            {1214L, "BODY-014", "外后视镜总成右", "MQB 电动折叠加热", "个", 2, "车身外饰"},
            {1215L, "BODY-015", "前大灯总成左", "MQB LED矩阵", "个", 2, "车身外饰"},
            {1216L, "BODY-016", "前大灯总成右", "MQB LED矩阵", "个", 2, "车身外饰"},
            {1217L, "BODY-017", "尾灯总成左", "MQB LED", "个", 2, "车身外饰"},
            {1218L, "BODY-018", "尾灯总成右", "MQB LED", "个", 2, "车身外饰"},
            {1219L, "BODY-019", "散热器格栅", "MQB 镀铬", "个", 2, "车身外饰"},
            {1220L, "BODY-020", "前轮眉内衬", "MQB 塑料", "个", 4, "车身外饰"},

            // ===== 车身内饰 =====
            {1301L, "INT-001", "仪表台总成", "MQB 搪塑", "台", 1, "车身内饰"},
            {1302L, "INT-002", "组合仪表总成", "MQB 全液晶", "台", 2, "车身内饰"},
            {1303L, "INT-003", "前座椅总成左", "MQB 电动调节加热", "个", 1, "车身内饰"},
            {1304L, "INT-004", "前座椅总成右", "MQB 电动调节加热", "个", 1, "车身内饰"},
            {1305L, "INT-005", "后座椅总成", "MQB 6/4分折", "套", 1, "车身内饰"},
            {1306L, "INT-006", "前排安全带", "MQB 预紧限力", "根", 4, "车身内饰"},
            {1307L, "INT-007", "门内饰板前左", "MQB PVC+织物", "个", 2, "车身内饰"},
            {1308L, "INT-008", "门内饰板前右", "MQB PVC+织物", "个", 2, "车身内饰"},
            {1309L, "INT-009", "顶棚总成", "MQB 无纺布", "个", 1, "车身内饰"},
            {1310L, "INT-010", "中控台扶手箱", "MQB 皮革包覆", "个", 2, "车身内饰"},

            // ===== 电器系统 =====
            {1401L, "ELEC-001", "发电机总成", "MQB 140A", "台", 1, "电器系统"},
            {1402L, "ELEC-002", "起动机总成", "MQB 1.4kW", "台", 1, "电器系统"},
            {1403L, "ELEC-003", "蓄电池AGM", "MQB 70Ah AGM", "个", 3, "电器系统"},
            {1404L, "ELEC-004", "雨刮电机总成", "MQB 前雨刮", "台", 2, "电器系统"},
            {1405L, "ELEC-005", "玻璃升降器前左", "MQB 电动", "个", 2, "电器系统"},
            {1406L, "ELEC-006", "玻璃升降器前右", "MQB 电动", "个", 2, "电器系统"},
            {1407L, "ELEC-007", "中控门锁前左", "MQB 电控", "个", 3, "电器系统"},
            {1408L, "ELEC-008", "整车线束总成", "MQB 主线束", "套", 1, "电器系统"},
            {1409L, "ELEC-009", "车身控制模块BCM", "MQB 高配", "台", 1, "电器系统"},
            {1410L, "ELEC-010", "网关控制单元", "MQB Gateway", "台", 1, "电器系统"},

            // ===== 安全系统 =====
            {1501L, "SAFE-001", "驾驶员安全气囊", "MQB 单级", "个", 2, "安全系统"},
            {1502L, "SAFE-002", "副驾驶安全气囊", "MQB 双级", "个", 2, "安全系统"},
            {1503L, "SAFE-003", "侧安全气囊左", "MQB 前排", "个", 2, "安全系统"},
            {1504L, "SAFE-004", "侧安全气囊右", "MQB 前排", "个", 2, "安全系统"},
            {1505L, "SAFE-005", "侧气帘左", "MQB 贯穿式", "个", 1, "安全系统"},
            {1506L, "SAFE-006", "侧气帘右", "MQB 贯穿式", "个", 1, "安全系统"},
            {1507L, "SAFE-007", "碰撞传感器前", "MQB 加速度式", "个", 3, "安全系统"},
            {1508L, "SAFE-008", "胎压传感器", "MQB 直接式TPMS", "个", 8, "安全系统"},
            {1509L, "SAFE-009", "安全气囊控制单元", "MQB ACU", "台", 1, "安全系统"},
            {1510L, "SAFE-010", "倒车雷达探头", "MQB 超声波", "个", 8, "安全系统"},
        };
        for (Object[] m : mats) {
            db.update("INSERT INTO materials(id,code,name,spec,unit,package_qty,category) VALUES(?,?,?,?,?,?,?)",
                m[0], m[1], m[2], m[3], m[4], m[5], m[6]);
        }
        System.out.println("=== 物料种子数据: " + mats.length + " 种 ===");
    }

    // ===================== 供应商 =====================
    private void seedSuppliers() {
        Object[][] sups = {
            {1001L, "GYS001", "博世汽车部件(长春)有限公司", "王伟", "0431-88765001", "吉林省长春市汽车经济技术开发区丰采街188号", "吉林", "长春"},
            {1002L, "GYS002", "大陆汽车电子(长春)有限公司", "李强", "0431-88765002", "吉林省长春市高新技术产业开发区硅谷大街3999号", "吉林", "长春"},
            {1003L, "GYS003", "采埃孚传动技术(上海)有限公司", "张明", "021-58665003", "上海市嘉定区安亭镇安拓路56号", "上海", "上海"},
            {1004L, "GYS004", "佛吉亚排气系统(长春)有限公司", "刘洋", "0431-88765004", "吉林省长春市绿园区西新镇开元路99号", "吉林", "长春"},
            {1005L, "GYS005", "法雷奥汽车电器(武汉)有限公司", "陈亮", "027-87665005", "湖北省武汉市汉南区纱帽街兴城大道388号", "湖北", "武汉"},
            {1006L, "GYS006", "麦格纳汽车外饰(长春)有限公司", "赵磊", "0431-88765006", "吉林省长春市经济开发区大连路288号", "吉林", "长春"},
            {1007L, "GYS007", "天合汽车安全系统(上海)有限公司", "孙涛", "021-58665007", "上海市浦东新区康桥工业区康柳路118号", "上海", "上海"},
            {1008L, "GYS008", "德尔福派克电气系统(长春)有限公司", "周杰", "0431-88765008", "吉林省长春市绿园区西安大路3888号", "吉林", "长春"},
            {1009L, "GYS009", "海拉车灯(长春)有限公司", "吴刚", "0431-88765009", "吉林省长春市汽车经济技术开发区东风大街999号", "吉林", "长春"},
            {1010L, "GYS010", "舍弗勒(中国)有限公司", "郑强", "0512-57665010", "江苏省苏州市太仓市宁波东路68号", "江苏", "苏州"},
            {1011L, "GYS011", "蒂森克虏伯汽车零部件(上海)有限公司", "冯浩", "021-58665011", "上海市奉贤区南桥镇环城北路1288号", "上海", "上海"},
            {1012L, "GYS012", "伟世通汽车电子(长春)有限公司", "马超", "0431-88765012", "吉林省长春市朝阳区硅谷大街2999号", "吉林", "长春"},
            {1013L, "GYS013", "李尔汽车座椅(长春)有限公司", "宋博", "0431-88765013", "吉林省长春市高新技术产业开发区光谷大街3888号", "吉林", "长春"},
            {1014L, "GYS014", "博格华纳传动系统(大连)有限公司", "韩磊", "0411-87665014", "辽宁省大连市金州区经济技术开发区淮河中路88号", "辽宁", "大连"},
            {1015L, "GYS015", "马勒发动机零部件(上海)有限公司", "林峰", "021-58665015", "上海市嘉定区徐行镇徐潘路258号", "上海", "上海"},
            {1016L, "GYS016", "康明斯涡轮增压技术(无锡)有限公司", "唐辉", "0510-85665016", "江苏省无锡市新区长江路28号", "江苏", "无锡"},
            {1017L, "GYS017", "均胜汽车安全系统(宁波)有限公司", "方明", "0574-87665017", "浙江省宁波市鄞州区潘火街道下应北路789号", "浙江", "宁波"},
            {1018L, "GYS018", "福耀玻璃工业集团(长春)有限公司", "曹征", "0431-88765018", "吉林省长春市经济开发区世纪大街1666号", "吉林", "长春"},
            {1019L, "GYS019", "万向钱潮传动轴(杭州)有限公司", "沈建", "0571-87665019", "浙江省杭州市萧山区万向路1号", "浙江", "杭州"},
            {1020L, "GYS020", "华域汽车系统(上海)有限公司", "蒋鹏", "021-58665020", "上海市嘉定区安亭镇曹安路5588号", "上海", "上海"},
        };
        for (Object[] s : sups) {
            db.update("INSERT INTO suppliers(id,code,name,contact_name,contact_phone,address,province,city) VALUES(?,?,?,?,?,?,?,?)",
                s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7]);
        }
        System.out.println("=== 供应商种子数据: " + sups.length + " 家 ===");
    }

    // ===================== 客户（一汽大众经销商） =====================
    private void seedCustomers() {
        Object[][] custs = {
            {3001L, "KH001", "一汽大众销售有限责任公司", "赵总经理", "0431-85908888", "吉林省长春市汽车经济技术开发区东风大街1999号", "吉林", "长春"},
            {3002L, "KH002", "长春通立汽车销售服务有限公司", "钱经理", "0431-84626888", "吉林省长春市绿园区正阳街4288号", "吉林", "长春"},
            {3003L, "KH003", "北京中润发汽车销售有限公司", "孙经理", "010-67889888", "北京市朝阳区北苑路168号", "北京", "北京"},
            {3004L, "KH004", "上海永达汽车销售服务有限公司", "李经理", "021-58559888", "上海市浦东新区张杨路2758号", "上海", "上海"},
            {3005L, "KH005", "广州大吉汽车销售服务有限公司", "周经理", "020-87239888", "广东省广州市白云区白云大道北1375号", "广东", "广州"},
            {3006L, "KH006", "成都新双立汽车销售服务有限公司", "吴经理", "028-85129888", "四川省成都市武侯区机场路68号", "四川", "成都"},
            {3007L, "KH007", "深圳昊天林汽车销售服务有限公司", "郑经理", "0755-83709888", "广东省深圳市福田区香蜜湖路6028号", "广东", "深圳"},
            {3008L, "KH008", "武汉恒信汽车销售服务有限公司", "王经理", "027-84897888", "湖北省武汉市汉阳区龙阳大道86号", "湖北", "武汉"},
            {3009L, "KH009", "杭州百得利汽车销售服务有限公司", "冯经理", "0571-88997888", "浙江省杭州市拱墅区石祥路471号", "浙江", "杭州"},
            {3010L, "KH010", "南京协众汽车销售服务有限公司", "陈经理", "025-85869888", "江苏省南京市江宁区天元东路228号", "江苏", "南京"},
            {3011L, "KH011", "郑州威佳汽车销售服务有限公司", "褚经理", "0371-65667888", "河南省郑州市金水区花园路北段88号", "河南", "郑州"},
            {3012L, "KH012", "天津捷通汽车销售服务有限公司", "卫经理", "022-84997888", "天津市西青区中北镇汽车园南路8号", "天津", "天津"},
            {3013L, "KH013", "重庆商社汽车销售服务有限公司", "蒋经理", "023-68892888", "重庆市渝北区龙溪街道红锦大道566号", "重庆", "重庆"},
            {3014L, "KH014", "西安新丰泰汽车销售服务有限公司", "沈经理", "029-86269888", "陕西省西安市未央区明光路98号", "陕西", "西安"},
            {3015L, "KH015", "济南庞大汽车销售服务有限公司", "韩经理", "0531-87509888", "山东省济南市槐荫区经十西路3888号", "山东", "济南"},
            {3016L, "KH016", "哈尔滨龙海汽车销售服务有限公司", "杨经理", "0451-86809888", "黑龙江省哈尔滨市道里区机场路18号", "黑龙江", "哈尔滨"},
            {3017L, "KH017", "沈阳和泰汽车销售服务有限公司", "朱经理", "024-86509888", "辽宁省沈阳市铁西区北二中路33号", "辽宁", "沈阳"},
            {3018L, "KH018", "长沙华洋汽车销售服务有限公司", "秦经理", "0731-82809888", "湖南省长沙市岳麓区麓谷大道668号", "湖南", "长沙"},
            {3019L, "KH019", "合肥宝荣汽车销售服务有限公司", "尤经理", "0551-63669888", "安徽省合肥市包河区繁华大道288号", "安徽", "合肥"},
            {3020L, "KH020", "福州永达汽车销售服务有限公司", "许经理", "0591-83509888", "福建省福州市仓山区福峡路88号", "福建", "福州"},
        };
        for (Object[] c : custs) {
            db.update("INSERT INTO customers(id,code,name,contact_name,contact_phone,address,province,city) VALUES(?,?,?,?,?,?,?,?)",
                c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]);
        }
        System.out.println("=== 客户种子数据: " + custs.length + " 家 ===");
    }

    // ===================== 仓库 =====================
    private void seedWarehouses() {
        Object[][] whs = {
            {4001L, "CK001", "长春一厂总装仓库", "吉林省长春市汽车经济技术开发区东风大街1号", "长春", "总装仓库"},
            {4002L, "CK002", "长春二厂总装仓库", "吉林省长春市绿园区西新经济技术开发区", "长春", "总装仓库"},
            {4003L, "CK003", "长春发动机工厂仓库", "吉林省长春市汽车经济技术开发区大众路888号", "长春", "发动机仓库"},
            {4004L, "CK004", "长春传动器工厂仓库", "吉林省长春市绿园区长白公路18号", "长春", "传动器仓库"},
            {4005L, "CK005", "成都工厂总装仓库", "四川省成都市龙泉驿区车城东七路368号", "成都", "总装仓库"},
            {4006L, "CK006", "佛山工厂总装仓库", "广东省佛山市南海区狮山镇虹岭路1号", "佛山", "总装仓库"},
            {4007L, "CK007", "天津工厂总装仓库", "天津市滨海新区经济技术开发区泰达大街88号", "天津", "总装仓库"},
            {4008L, "CK008", "青岛工厂总装仓库", "山东省青岛市即墨区汽车产业新城大众一路1号", "青岛", "总装仓库"},
            {4009L, "CK009", "长春冲压中心仓库", "吉林省长春市汽车经济技术开发区富奥路666号", "长春", "冲压仓库"},
            {4010L, "CK010", "长春备件中心仓库", "吉林省长春市宽城区北环城路2888号", "长春", "备件仓库"},
        };
        for (Object[] w : whs) {
            db.update("INSERT INTO warehouses(id,code,name,address,city,type) VALUES(?,?,?,?,?,?)",
                w[0], w[1], w[2], w[3], w[4], w[5]);
        }
        System.out.println("=== 仓库种子数据: " + whs.length + " 个 ===");
    }

    // ===================== 库位（每个仓库多个库位） =====================
    private void seedLocations() {
        Object[][] locs = {
            // 长春一厂总装仓库
            {5001L, 4001L, "KW-1A-A01", "A区01库位", "A", "01", "1"},
            {5002L, 4001L, "KW-1A-A02", "A区02库位", "A", "01", "2"},
            {5003L, 4001L, "KW-1A-A03", "A区03库位", "A", "02", "1"},
            {5004L, 4001L, "KW-1A-B01", "B区01库位", "B", "01", "1"},
            {5005L, 4001L, "KW-1A-B02", "B区02库位", "B", "01", "2"},
            {5006L, 4001L, "KW-1A-C01", "C区01库位", "C", "01", "1"},
            // 长春二厂总装仓库
            {5007L, 4002L, "KW-2A-A01", "A区01库位", "A", "01", "1"},
            {5008L, 4002L, "KW-2A-A02", "A区02库位", "A", "01", "2"},
            {5009L, 4002L, "KW-2A-B01", "B区01库位", "B", "01", "1"},
            {5010L, 4002L, "KW-2A-B02", "B区02库位", "B", "01", "2"},
            // 长春发动机仓库
            {5011L, 4003L, "KW-FDJ-A01", "发动机A区01库位", "A", "01", "1"},
            {5012L, 4003L, "KW-FDJ-A02", "发动机A区02库位", "A", "01", "2"},
            {5013L, 4003L, "KW-FDJ-B01", "发动机B区01库位", "B", "02", "1"},
            // 长春传动器仓库
            {5014L, 4004L, "KW-CDQ-A01", "传动器A区01库位", "A", "01", "1"},
            {5015L, 4004L, "KW-CDQ-A02", "传动器A区02库位", "A", "01", "2"},
            // 成都工厂仓库
            {5016L, 4005L, "KW-CDA-A01", "成都A区01库位", "A", "01", "1"},
            {5017L, 4005L, "KW-CDA-A02", "成都A区02库位", "A", "01", "2"},
            {5018L, 4005L, "KW-CDA-B01", "成都B区01库位", "B", "02", "1"},
            // 佛山工厂仓库
            {5019L, 4006L, "KW-FSA-A01", "佛山A区01库位", "A", "01", "1"},
            {5020L, 4006L, "KW-FSA-B01", "佛山B区01库位", "B", "02", "1"},
            // 天津工厂仓库
            {5021L, 4007L, "KW-TJA-A01", "天津A区01库位", "A", "01", "1"},
            {5022L, 4007L, "KW-TJA-B01", "天津B区01库位", "B", "02", "1"},
            // 青岛工厂仓库
            {5023L, 4008L, "KW-QDA-A01", "青岛A区01库位", "A", "01", "1"},
            {5024L, 4008L, "KW-QDA-B01", "青岛B区01库位", "B", "02", "1"},
            // 长春冲压中心
            {5025L, 4009L, "KW-CYA-A01", "冲压A区01库位", "A", "01", "1"},
            {5026L, 4009L, "KW-CYA-B01", "冲压B区01库位", "B", "02", "1"},
            // 长春备件中心
            {5027L, 4010L, "KW-BJA-A01", "备件A区01库位", "A", "01", "1"},
            {5028L, 4010L, "KW-BJA-A02", "备件A区02库位", "A", "01", "2"},
            {5029L, 4010L, "KW-BJA-B01", "备件B区01库位", "B", "02", "1"},
            {5030L, 4010L, "KW-BJA-C01", "备件C区01库位", "C", "03", "1"},
        };
        for (Object[] l : locs) {
            db.update("INSERT INTO locations(id,warehouse_id,code,name,area,shelf,layer) VALUES(?,?,?,?,?,?,?)",
                l[0], l[1], l[2], l[3], l[4], l[5], l[6]);
        }
        System.out.println("=== 库位种子数据: " + locs.length + " 个 ===");
    }

    // ===================== 用户 =====================
    private void seedUsers() {
        db.update("INSERT INTO users(id,username,password,nickname,role) VALUES(?,?,?,?,?)",
            9001L, "admin", "123456", "系统管理员", "admin");
        db.update("INSERT INTO users(id,username,password,nickname,role) VALUES(?,?,?,?,?)",
            9002L, "zhangsan", "123456", "张三", "operator");
        db.update("INSERT INTO users(id,username,password,nickname,role) VALUES(?,?,?,?,?)",
            9003L, "lisi", "123456", "李四", "operator");
        System.out.println("=== 用户种子数据: 3 人 ===");
    }
}
