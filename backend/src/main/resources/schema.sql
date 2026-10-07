-- ============================================================================
-- WMS 仓储管理系统 —— MySQL 8.0 表结构
--
-- 设计要点：
--   1. 主键统一用 BIGINT AUTO_INCREMENT，不再由应用层发号。
--   2. 业务表不再冗余存物料名/仓库名/库位名，改为查询时 JOIN 派生，
--      保证单一数据来源；对外字段名保持不变，前端无感知。
--   3. 每张表带 NOT NULL / UNIQUE / FOREIGN KEY / CHECK 约束。
--   4. 幂等：全部使用 CREATE TABLE IF NOT EXISTS，可重复执行。
--
-- 本文件由 Spring Boot 启动时执行（spring.sql.init.mode=always），
-- 也可手工执行：mysql -u wms -p wms < schema.sql
-- ============================================================================

-- ========================= 基础主数据 =========================

CREATE TABLE IF NOT EXISTS suppliers (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL                COMMENT '供应商编码',
    name          VARCHAR(150) NOT NULL                COMMENT '供应商名称',
    contact_name  VARCHAR(50)  NULL,
    contact_phone VARCHAR(30)  NULL,
    address       VARCHAR(200) NULL,
    province      VARCHAR(50)  NULL,
    city          VARCHAR(50)  NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_code (code),
    KEY idx_supplier_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '供应商';

CREATE TABLE IF NOT EXISTS customers (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)  NOT NULL                COMMENT '客户编码',
    name          VARCHAR(150) NOT NULL                COMMENT '客户名称',
    contact_name  VARCHAR(50)  NULL,
    contact_phone VARCHAR(30)  NULL,
    address       VARCHAR(200) NULL,
    province      VARCHAR(50)  NULL,
    city          VARCHAR(50)  NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_code (code),
    KEY idx_customer_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户';

CREATE TABLE IF NOT EXISTS warehouses (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(50)  NOT NULL                   COMMENT '仓库编码',
    name       VARCHAR(150) NOT NULL                   COMMENT '仓库名称',
    address    VARCHAR(200) NULL,
    city       VARCHAR(50)  NULL,
    type       VARCHAR(30)  NULL                       COMMENT '仓库类型：总装/发动机/冲压/备件…',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (code),
    KEY idx_warehouse_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '仓库';

CREATE TABLE IF NOT EXISTS locations (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    warehouse_id BIGINT       NOT NULL                 COMMENT '所属仓库',
    code         VARCHAR(50)  NOT NULL                 COMMENT '库位编码',
    name         VARCHAR(100) NOT NULL                 COMMENT '库位名称',
    area         VARCHAR(20)  NULL                     COMMENT '区域',
    shelf        VARCHAR(20)  NULL                     COMMENT '货架',
    layer        VARCHAR(20)  NULL                     COMMENT '层',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 同一仓库内库位编码唯一
    UNIQUE KEY uk_location_warehouse_code (warehouse_id, code),
    KEY idx_location_warehouse (warehouse_id),
    CONSTRAINT fk_location_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库位';

CREATE TABLE IF NOT EXISTS materials (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)   NOT NULL                 COMMENT '物料编码',
    name        VARCHAR(100)  NOT NULL                 COMMENT '物料名称',
    spec        VARCHAR(100)  NULL                     COMMENT '规格型号',
    unit        VARCHAR(20)   NULL                     COMMENT '单位',
    package_qty INT           NOT NULL DEFAULT 0       COMMENT '标准包装容量，0 表示不拆包',
    category    VARCHAR(50)   NULL                     COMMENT '物料分类',
    min_stock   DECIMAL(12,2) NOT NULL DEFAULT 0       COMMENT '库存下限',
    max_stock   DECIMAL(12,2) NOT NULL DEFAULT 9999    COMMENT '库存上限',
    supplier_id BIGINT        NULL                     COMMENT '默认供应商',
    price       DECIMAL(12,2) NOT NULL DEFAULT 0       COMMENT '单价',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_code (code),
    KEY idx_material_name (name),
    KEY idx_material_category (category),
    KEY idx_material_supplier (supplier_id),
    CONSTRAINT fk_material_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_material_stock CHECK (min_stock >= 0 AND max_stock >= 0
                                        AND package_qty >= 0 AND price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物料主数据';

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50) NOT NULL                    COMMENT '登录名',
    password   VARCHAR(100) NOT NULL                   COMMENT '口令',
    nickname   VARCHAR(50) NULL,
    role       VARCHAR(30) NOT NULL DEFAULT 'operator' COMMENT '角色：admin/operator',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    CONSTRAINT ck_user_role CHECK (role IN ('admin', 'operator'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

-- ========================= 入库流程 =========================

CREATE TABLE IF NOT EXISTS inbound_orders (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(50)  NOT NULL               COMMENT '入库单号',
    inbound_type   VARCHAR(30)  NOT NULL DEFAULT '采购入库',
    supplier_id    BIGINT       NULL                   COMMENT '供应商；退货入库时存客户 id',
    warehouse_id   BIGINT       NULL                   COMMENT '默认收货仓库',
    status         VARCHAR(20)  NOT NULL DEFAULT '待入库',
    plan_date      DATE         NULL                   COMMENT '计划到货日期',
    create_by      VARCHAR(50)  NULL                   COMMENT '制单人',
    reverse_reason VARCHAR(300) NULL                   COMMENT '反审核原因',
    remark         VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbound_order_no (order_no),
    KEY idx_inbound_status (status),
    KEY idx_inbound_supplier (supplier_id),
    KEY idx_inbound_plan_date (plan_date),
    CONSTRAINT fk_inbound_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_inbound_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_inbound_status CHECK (status IN ('草稿', '待入库', '部分入库', '已完成', '已撤销'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '入库单';

CREATE TABLE IF NOT EXISTS inbound_order_items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    material_id  BIGINT        NOT NULL,
    plan_qty     DECIMAL(12,2) NOT NULL DEFAULT 0      COMMENT '计划数量',
    received_qty DECIMAL(12,2) NOT NULL DEFAULT 0      COMMENT '已入库数量',
    batch_no     VARCHAR(100)  NULL                    COMMENT '批次号',
    location_id  BIGINT        NULL                    COMMENT '收货库位',
    warehouse_id BIGINT        NULL                    COMMENT '收货仓库',
    status       VARCHAR(20)   NOT NULL DEFAULT '待入库',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ioi_order (order_id),
    KEY idx_ioi_material (material_id),
    KEY idx_ioi_batch (batch_no),
    CONSTRAINT fk_ioi_order FOREIGN KEY (order_id)
        REFERENCES inbound_orders (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ioi_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ioi_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_ioi_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_ioi_qty CHECK (plan_qty >= 0 AND received_qty >= 0 AND received_qty <= plan_qty),
    CONSTRAINT ck_ioi_status CHECK (status IN ('待入库', '部分入库', '已完成'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '入库单明细';

CREATE TABLE IF NOT EXISTS kanbans (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    kanban_code     VARCHAR(50)   NOT NULL             COMMENT '看板码（打印二维码内容）',
    order_id        BIGINT        NULL,
    order_item_id   BIGINT        NULL,
    material_id     BIGINT        NULL,
    plan_qty        DECIMAL(12,2) NOT NULL DEFAULT 0,
    received_qty    DECIMAL(12,2) NOT NULL DEFAULT 0,
    warehouse_id    BIGINT        NULL,
    location_id     BIGINT        NULL,
    batch_no        VARCHAR(100)  NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT '未打印',
    print_count     INT           NOT NULL DEFAULT 0,
    package_info    VARCHAR(50)   NULL                 COMMENT '包装序号，如 2/6',
    last_print_time DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_kanban_code (kanban_code),
    KEY idx_kanban_order (order_id),
    KEY idx_kanban_item (order_item_id),
    KEY idx_kanban_material (material_id),
    KEY idx_kanban_status (status),
    KEY idx_kanban_batch (batch_no),
    -- 单据或明细删除时，其看板随之删除
    CONSTRAINT fk_kanban_order FOREIGN KEY (order_id)
        REFERENCES inbound_orders (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_kanban_item FOREIGN KEY (order_item_id)
        REFERENCES inbound_order_items (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_kanban_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_kanban_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_kanban_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_kanban_qty CHECK (plan_qty >= 0 AND received_qty >= 0),
    CONSTRAINT ck_kanban_print_count CHECK (print_count >= 0),
    CONSTRAINT ck_kanban_status CHECK (status IN ('未打印', '已打印', '部分入库', '已完成', '部分出库', '已出库'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '入库看板（打印/扫码标签）';

-- ========================= 库存与流水 =========================

CREATE TABLE IF NOT EXISTS inventories (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    material_id  BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL                COMMENT '所在仓库',
    location_id  BIGINT        NOT NULL                COMMENT '所在库位',
    batch_no     VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '批次号',
    qty          DECIMAL(12,2) NOT NULL DEFAULT 0      COMMENT '库存数量',
    frozen_qty   DECIMAL(12,2) NOT NULL DEFAULT 0      COMMENT '封存数量',
    package_qty  DECIMAL(12,2) NOT NULL DEFAULT 0      COMMENT '转包后包装容量',
    status       VARCHAR(20)   NOT NULL DEFAULT '已入库',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 库存唯一键：同一物料+仓库+库位+批次只允许一行
    UNIQUE KEY uk_inventory_key (material_id, warehouse_id, location_id, batch_no),
    KEY idx_inventory_warehouse (warehouse_id),
    KEY idx_inventory_location (location_id),
    KEY idx_inventory_status (status),
    CONSTRAINT fk_inventory_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    -- 有库存的仓库/库位不允许直接删除
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_inventory_qty CHECK (qty >= 0 AND frozen_qty >= 0 AND frozen_qty <= qty),
    CONSTRAINT ck_inventory_status CHECK (status IN ('已入库', '已耗尽'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存';

CREATE TABLE IF NOT EXISTS records (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    record_no     VARCHAR(50)   NOT NULL               COMMENT '流水号',
    business_type VARCHAR(20)   NOT NULL               COMMENT '入库/出库/封存/解封/转包/盘点/反审…',
    business_no   VARCHAR(50)   NULL                   COMMENT '关联单据号',
    kanban_code   VARCHAR(50)   NULL,
    material_id   BIGINT        NULL,
    warehouse_id  BIGINT        NULL,
    location_id   BIGINT        NULL,
    batch_no      VARCHAR(100)  NULL,
    change_qty    DECIMAL(12,2) NULL                   COMMENT '变动数量，入库为正、出库为负',
    before_qty    DECIMAL(12,2) NULL,
    after_qty     DECIMAL(12,2) NULL,
    operator      VARCHAR(50)   NULL,
    remark        VARCHAR(500)  NULL,
    operate_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_record_kanban (kanban_code),
    KEY idx_record_material (material_id),
    KEY idx_record_time (operate_time),
    KEY idx_record_business (business_type, business_no),
    KEY idx_record_batch (batch_no),
    CONSTRAINT fk_record_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_record_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_record_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存流水台账';

-- ========================= 出库流程 =========================

CREATE TABLE IF NOT EXISTS outbound_orders (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(50)  NOT NULL               COMMENT '出库单号',
    outbound_type  VARCHAR(30)  NOT NULL DEFAULT '销售出库',
    customer_id    BIGINT       NULL,
    warehouse_id   BIGINT       NULL                   COMMENT '发货仓库',
    status         VARCHAR(20)  NOT NULL DEFAULT '待出库',
    plan_date      DATE         NULL                   COMMENT '计划发货日期',
    create_by      VARCHAR(50)  NULL,
    reverse_reason VARCHAR(300) NULL,
    remark         VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbound_order_no (order_no),
    KEY idx_outbound_status (status),
    KEY idx_outbound_customer (customer_id),
    KEY idx_outbound_plan_date (plan_date),
    CONSTRAINT fk_outbound_customer FOREIGN KEY (customer_id)
        REFERENCES customers (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_outbound_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_outbound_status CHECK (status IN ('草稿', '待出库', '部分出库', '已完成', '已撤销'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出库单';

CREATE TABLE IF NOT EXISTS outbound_order_items (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    order_id    BIGINT        NOT NULL,
    material_id BIGINT        NOT NULL,
    plan_qty    DECIMAL(12,2) NOT NULL DEFAULT 0       COMMENT '计划出库数量',
    shipped_qty DECIMAL(12,2) NOT NULL DEFAULT 0       COMMENT '已出库数量',
    batch_no    VARCHAR(100)  NULL                     COMMENT '指定批次',
    status      VARCHAR(20)   NOT NULL DEFAULT '待出库',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ooi_order (order_id),
    KEY idx_ooi_material (material_id),
    CONSTRAINT fk_ooi_order FOREIGN KEY (order_id)
        REFERENCES outbound_orders (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ooi_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_ooi_qty CHECK (plan_qty >= 0 AND shipped_qty >= 0 AND shipped_qty <= plan_qty),
    CONSTRAINT ck_ooi_status CHECK (status IN ('待出库', '部分出库', '已完成'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出库单明细';

-- ========================= 封存 / 转包 =========================

CREATE TABLE IF NOT EXISTS freeze_records (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    freeze_no          VARCHAR(50)   NOT NULL          COMMENT '封存单号',
    inventory_id       BIGINT        NULL,
    material_id        BIGINT        NULL,
    warehouse_id       BIGINT        NULL,
    location_id        BIGINT        NULL,
    batch_no           VARCHAR(100)  NULL,
    freeze_qty         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '本次封存数量',
    before_frozen_qty  DECIMAL(12,2) NOT NULL DEFAULT 0,
    after_frozen_qty   DECIMAL(12,2) NOT NULL DEFAULT 0,
    unfrozen_qty       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计已解封数量',
    freeze_reason      VARCHAR(300)  NULL,
    operator           VARCHAR(50)   NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT '已冻结',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_freeze_no (freeze_no),
    KEY idx_freeze_material (material_id),
    KEY idx_freeze_status (status),
    KEY idx_freeze_inventory (inventory_id),
    CONSTRAINT fk_freeze_inventory FOREIGN KEY (inventory_id)
        REFERENCES inventories (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_freeze_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_freeze_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_freeze_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_freeze_qty CHECK (freeze_qty > 0 AND unfrozen_qty >= 0
                                    AND unfrozen_qty <= after_frozen_qty),
    CONSTRAINT ck_freeze_status CHECK (status IN ('已冻结', '已解冻'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存封存记录';

CREATE TABLE IF NOT EXISTS unfreeze_records (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    freeze_id        BIGINT        NOT NULL            COMMENT '所属封存记录',
    unfreeze_qty     DECIMAL(12,2) NOT NULL DEFAULT 0,
    unfreeze_reason  VARCHAR(300)  NULL,
    operator         VARCHAR(50)   NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_unfreeze_freeze (freeze_id),
    CONSTRAINT fk_unfreeze_freeze FOREIGN KEY (freeze_id)
        REFERENCES freeze_records (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_unfreeze_qty CHECK (unfreeze_qty > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存解封记录';

CREATE TABLE IF NOT EXISTS repack_records (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    repack_no           VARCHAR(50)   NOT NULL         COMMENT '转包单号',
    source_inventory_id BIGINT        NULL,
    target_inventory_id BIGINT        NULL,
    material_id         BIGINT        NULL,
    to_warehouse_id     BIGINT        NULL,
    to_location_id      BIGINT        NULL,
    from_batch_no       VARCHAR(100)  NULL,
    to_batch_no         VARCHAR(100)  NULL,
    repack_qty          DECIMAL(12,2) NOT NULL DEFAULT 0,
    before_qty          DECIMAL(12,2) NULL             COMMENT '源库存转包前数量',
    after_qty           DECIMAL(12,2) NULL             COMMENT '源库存转包后数量',
    package_qty         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '目标包装容量',
    operator            VARCHAR(50)   NULL,
    reason              VARCHAR(300)  NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repack_no (repack_no),
    KEY idx_repack_material (material_id),
    KEY idx_repack_source (source_inventory_id),
    KEY idx_repack_target (target_inventory_id),
    CONSTRAINT fk_repack_source FOREIGN KEY (source_inventory_id)
        REFERENCES inventories (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_repack_target FOREIGN KEY (target_inventory_id)
        REFERENCES inventories (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_repack_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_repack_to_warehouse FOREIGN KEY (to_warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_repack_to_location FOREIGN KEY (to_location_id)
        REFERENCES locations (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_repack_qty CHECK (repack_qty > 0 AND package_qty >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存转包记录';

-- ========================= 库存预警 =========================

CREATE TABLE IF NOT EXISTS alert_logs (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    alert_no       VARCHAR(50)   NOT NULL              COMMENT '预警单号',
    alert_type     VARCHAR(30)   NOT NULL              COMMENT '缺货预警/呆滞预警',
    level          VARCHAR(20)   NOT NULL DEFAULT '中' COMMENT '高/中/低',
    status         VARCHAR(20)   NOT NULL DEFAULT '待处理',
    material_id    BIGINT        NULL,
    warehouse_id   BIGINT        NULL,
    location_id    BIGINT        NULL,
    batch_no       VARCHAR(100)  NULL,
    qty            DECIMAL(12,2) NULL                  COMMENT '当前库存量',
    available_qty  DECIMAL(12,2) NULL                  COMMENT '可用量（扣除封存）',
    threshold_qty  DECIMAL(12,2) NULL                  COMMENT '触发阈值',
    daily_avg_out  DECIMAL(12,2) NULL                  COMMENT '近 30 天日均出库',
    lead_time_days INT           NULL,
    stagnant_days  INT           NULL,
    message        VARCHAR(500)  NULL,
    suggestion     VARCHAR(500)  NULL,
    handler        VARCHAR(50)   NULL,
    action         VARCHAR(100)  NULL,
    remark         VARCHAR(500)  NULL,
    handled_at     DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_no (alert_no),
    -- 同一物料+批次+类型只保留一条未处理预警，支撑 upsert 语义
    KEY idx_alert_open (alert_type, material_id, batch_no, status),
    KEY idx_alert_status (status),
    KEY idx_alert_material (material_id),
    CONSTRAINT fk_alert_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_alert_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_alert_location FOREIGN KEY (location_id)
        REFERENCES locations (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_alert_status CHECK (status IN ('待处理', '已处理', '已忽略')),
    CONSTRAINT ck_alert_level CHECK (level IN ('高', '中', '低')),
    CONSTRAINT ck_alert_type CHECK (alert_type IN ('缺货预警', '呆滞预警'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存预警日志';

CREATE TABLE IF NOT EXISTS alert_configs (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    material_id    BIGINT   NOT NULL                   COMMENT '适用物料；每物料一条配置',
    lead_time_days INT      NOT NULL DEFAULT 7         COMMENT '到货周期天数',
    stagnant_days  INT      NOT NULL DEFAULT 180       COMMENT '呆滞判定天数',
    enabled        TINYINT  NOT NULL DEFAULT 1         COMMENT '1 启用 0 停用',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_config_material (material_id),
    CONSTRAINT fk_alert_config_material FOREIGN KEY (material_id)
        REFERENCES materials (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_alert_config_days CHECK (lead_time_days >= 0 AND stagnant_days >= 0),
    CONSTRAINT ck_alert_config_enabled CHECK (enabled IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '库存预警阈值配置';

-- ========================= 出库策略 / PDA 离线缓存 =========================

CREATE TABLE IF NOT EXISTS fifo_configs (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    mode       VARCHAR(20) NOT NULL                   COMMENT 'strict=严格先进先出 loose=宽松',
    active     CHAR(1)     NOT NULL DEFAULT 'N'       COMMENT 'Y=当前生效配置，同一时刻只应有一条 Y',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fifo_active (active),
    CONSTRAINT ck_fifo_mode CHECK (mode IN ('strict', 'loose')),
    CONSTRAINT ck_fifo_active CHECK (active IN ('Y', 'N'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出库先进先出策略配置';

CREATE TABLE IF NOT EXISTS pda_caches (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    device_id   VARCHAR(64)  NOT NULL                  COMMENT 'PDA 设备号',
    op_type     VARCHAR(30)  NOT NULL                  COMMENT '离线操作类型',
    data        TEXT         NULL                      COMMENT '操作载荷 JSON',
    sync_status VARCHAR(20)  NOT NULL DEFAULT 'pending',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pda_device (device_id, sync_status),
    CONSTRAINT ck_pda_sync_status CHECK (sync_status IN ('pending', 'synced'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'PDA 离线操作缓存';

-- ========================= 看板查询视图 =========================
-- 把原先冗余存储在各业务表里的物料/仓库/库位名称改为 JOIN 派生，
-- 视图列名与旧表结构保持一致，用于核对「同一份数据只有一个来源」。

CREATE OR REPLACE VIEW v_inventory AS
SELECT i.id,
       i.material_id,
       m.code                AS material_code,
       m.name                AS material_name,
       m.spec                AS spec,
       m.unit                AS unit,
       i.warehouse_id,
       w.name                AS warehouse_name,
       i.location_id,
       l.code                AS location_code,
       l.name                AS location_name,
       i.batch_no,
       i.qty,
       i.frozen_qty,
       (i.qty - i.frozen_qty) AS available_qty,
       i.package_qty,
       i.status,
       i.created_at,
       i.updated_at
FROM inventories i
         JOIN materials m ON m.id = i.material_id
         JOIN warehouses w ON w.id = i.warehouse_id
         JOIN locations l ON l.id = i.location_id;

CREATE OR REPLACE VIEW v_kanban AS
SELECT k.id,
       k.kanban_code,
       k.order_id,
       o.order_no,
       k.order_item_id,
       k.material_id,
       m.code      AS material_code,
       m.name      AS material_name,
       m.spec      AS spec,
       k.plan_qty,
       k.received_qty,
       k.warehouse_id,
       w.name      AS warehouse_name,
       k.location_id,
       l.name      AS location_name,
       k.batch_no,
       k.status,
       k.print_count,
       k.package_info,
       k.last_print_time,
       k.created_at,
       k.updated_at
FROM kanbans k
         LEFT JOIN inbound_orders o ON o.id = k.order_id
         LEFT JOIN materials m ON m.id = k.material_id
         LEFT JOIN warehouses w ON w.id = k.warehouse_id
         LEFT JOIN locations l ON l.id = k.location_id;
