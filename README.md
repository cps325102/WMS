# WMS 仓储管理系统

Vue3 + Element Plus 前端与 Spring Boot 后端组成的 WMS 演示系统，覆盖入库、出库、库存全流程管理。

## 目录结构

```text
WMS
├── db
│   └── setup.sql                  MySQL 建库 + 建应用账号脚本（用 root 执行一次）
├── backend                        Spring Boot 后端（Maven 工程）
│   ├── scripts/extract_seed.py    一次性脚本：把原 Java 种子数据导成 data.sql
│   └── src/main
│       ├── java/com/example/wms
│       └── resources
│           ├── application.yml     数据源与连接池配置
│           ├── schema.sql          表结构（含约束、索引、视图）
│           └── data.sql            种子数据
└── frontend                       Vue3 + Vite 前端
    ├── src
    ├── index.html
    └── vite.config.js
```

## 已完成功能

### 基础管理

- 登录打通
- 菜单栏与标签页联动
- 物料管理（含包装容量）
- 供应商管理
- 仓库管理
- 库位管理

### 入库管理

- 创建入库单
- 修改入库单
- 删除入库单
- 打印入库单
- 生成入库看板
- 打印入库看板
- 看板扫码入库
- 入库历史记录
- 看板生命周期查询（未打印 → 已打印 → 部分入库 → 已完成）

### 出库管理

- 创建出库单
- 修改出库单
- 删除出库单
- 扫码出库（扫描已完成入库的看板）
- 出库数量控制（不能超过出库单计划数量）
- 出库单进度跟踪（待出库 → 部分出库 → 已完成）
- 出库历史记录
- 自动更新库存和看板状态

### 库存管理

- 当前库存查询
- 库存监控（实时状态）
- 库存流水追溯（入库/出库完整记录）
- 库存耗尽自动标记
- 先进先出（FIFO）支持

### 数据追溯

- 库存流水追溯
- 看板追溯
- 业务类型区分（入库/出库）
- 数量变动可视化（绿色+ / 红色-）

### 其他模块

- 看板可视化（Dashboard）
- 库存监控与 AI 智能预警
- 看板封存 / 解封
- FIFO 配置
- 历史日志查询
- PDA 扫码
- 看板批量打印

## 技术栈

后端：Spring Boot 3、Java 17、Maven、Spring JDBC、MySQL 8

前端：Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios

## 数据库

MySQL 8.0，库名 `wms`，字符集 `utf8mb4`。表结构见 [backend/src/main/resources/schema.sql](backend/src/main/resources/schema.sql)。

### 表清单

| 分组 | 表 | 说明 |
| --- | --- | --- |
| 基础主数据 | `materials` | 物料，含包装容量与库存上下限 |
| | `suppliers` / `customers` | 供应商 / 客户 |
| | `warehouses` / `locations` | 仓库 / 库位（库位编码在同一仓库内唯一） |
| | `users` | 登录用户 |
| 入库 | `inbound_orders` / `inbound_order_items` | 入库单表头与明细 |
| | `kanbans` | 入库看板（打印与扫码标签） |
| 库存 | `inventories` | 库存，业务唯一键为 物料+仓库+库位+批次 |
| | `records` | 库存流水台账 |
| 出库 | `outbound_orders` / `outbound_order_items` | 出库单表头与明细 |
| 封存转包 | `freeze_records` / `unfreeze_records` | 封存与解封记录 |
| | `repack_records` | 转包记录 |
| 预警 | `alert_logs` / `alert_configs` | 预警日志与阈值配置 |
| 其他 | `fifo_configs` | 出库先进先出策略 |
| | `pda_caches` | PDA 离线操作缓存 |

另外提供两个只读视图便于核对数据：`v_inventory`、`v_kanban`。

### 设计要点

**主键与约束**：所有主键为 `BIGINT AUTO_INCREMENT`，由数据库分配。表间关系用外键约束表达，并配有 `NOT NULL`、`UNIQUE`、`CHECK` 约束。例如库存数量不允许为负、封存量不得超过库存量、状态值只能是预定义枚举、物料编码与单据号唯一。

**不存冗余名称**：物料名、仓库名、库位名等只在各自的表里存一份，业务表只保留外键 id，查询时通过 JOIN 派生。对外的 JSON 字段名与重构前保持一致，前端无需改动。

**幂等初始化**：`schema.sql` 全部使用 `CREATE TABLE IF NOT EXISTS`，`data.sql` 使用 `INSERT IGNORE`，因此随应用启动重复执行不会报错也不会产生重复数据。

## 测试账号

用户名：admin

密码：123456

## 启动方式

### 1. 初始化数据库（首次，用 root 执行一次）

```bash
mysql -u root -p < db/setup.sql
```

该脚本会创建 `wms` 库和专用账号。之后用这个账号即可连接查看数据库：

| 项 | 值 |
| --- | --- |
| 主机 | `localhost` |
| 端口 | `3306` |
| 数据库 | `wms` |
| 用户名 | `wms` |
| 密码 | `Wms@2026` |

### 2. 后端

进入 backend 目录：

```bash
mvn spring-boot:run
```

默认端口：

```text
8080
```

启动时会自动执行 `schema.sql` 建表、`data.sql` 写入演示数据，无需手工建表。

连接参数可用环境变量覆盖（默认值即上面的账号）：

```bash
WMS_DB_HOST=localhost
WMS_DB_PORT=3306
WMS_DB_NAME=wms
WMS_DB_USER=wms
WMS_DB_PASSWORD=Wms@2026
```

### 3. 前端

进入 frontend 目录：

```bash
npm install
npm run dev
```

默认端口：

```text
5173
```

浏览器访问：

```text
http://localhost:5173
```

前端已在 `vite.config.js` 中将 `/api` 代理到 `http://localhost:8080`，本地开发无需额外配置跨域。

## 主要接口

```text
POST /api/auth/login
GET  /api/menu/list
GET  /api/basic/{type}/list
POST /api/basic/{type}/save
DELETE /api/basic/{type}/delete/{id}
POST /api/inbound/order/create
PUT  /api/inbound/order/update/{id}
GET  /api/inbound/order/list
GET  /api/inbound/order/detail/{id}
DELETE /api/inbound/order/delete/{id}
GET  /api/inbound/order/print/{id}
POST /api/inbound/kanban/generate/{orderId}
GET  /api/inbound/kanban/list
GET  /api/inbound/kanban/print/{id}
GET  /api/inbound/kanban/scan/{kanbanCode}
POST /api/inbound/receive
GET  /api/inventory/list
GET  /api/inventory/trace
GET  /api/inbound/kanban/trace/{kanbanCode}
```

## 开发说明

- 表结构与种子数据分别放在 `src/main/resources/schema.sql` 和 `data.sql`，由 Spring Boot 启动时执行，不再用 Java 代码建表。
- 数据访问统一收敛在 `storage/DataStorage`，全部使用 `?` 占位符传参，不做 SQL 字符串拼接。
- 涉及多表写入的业务方法（扫码入库、执行出库、封存/解封/转包、反审核等）都标了 `@Transactional`，中途失败整体回滚。
- 异常统一由 `config/GlobalExceptionHandler` 转成前端的 `{code, message, data}` 结构。
- 前端请求统一封装在 `src/utils/request.js`，开发环境由 Vite 代理转发到后端。
- 业务数据全部走数据库读写，重启后不会丢失。
- `backend/scripts/extract_seed.py` 是把原始 Java 种子数据搬到 `data.sql` 的一次性脚本，数据已生成完毕，平时不需要再跑。

### 数据库连接排查

如果启动时报数据库连接失败，按顺序检查：

1. MySQL 服务是否在运行（Windows 服务名通常为 `MySQL80`）。
2. `db/setup.sql` 是否已用 root 执行过，`wms` 账号是否创建成功。
3. `application.yml` 里的连接参数或对应环境变量是否正确。
