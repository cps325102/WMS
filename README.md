# WMS 仓储管理系统

Vue3 + Element Plus 前端与 Spring Boot 后端组成的 WMS 演示系统，覆盖入库、出库、库存全流程管理。

## 目录结构

```text
WMS
├── backend                        Spring Boot 后端（Maven 工程）
│   ├── src/main/java/com/example/wms
│   ├── src/main/resources/application.yml
│   └── data                       H2 本地库文件（运行时生成）
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

后端：Spring Boot 3、Java 17、Maven、Spring JDBC、H2 / MySQL

前端：Vue 3、Vite、Element Plus、Vue Router、Pinia、Axios

## 测试账号

用户名：admin

密码：123456

## 启动方式

### 后端

进入 backend 目录：

```bash
mvn spring-boot:run
```

默认端口：

```text
8080
```

数据默认存放在本地 H2 文件库 `backend/data/wms`，首次启动自动建表并写入演示数据，无需额外安装数据库。

如需切换 MySQL，在启动前设置环境变量：

```bash
WMS_DB=mysql
WMS_MYSQL_URL=jdbc:mysql://localhost:3306/wms?useSSL=false&serverTimezone=Asia/Shanghai
WMS_MYSQL_USER=root
WMS_MYSQL_PASSWORD=你的密码
```

### 前端

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

- 表结构在 `config/DatabaseInitializer` 中以代码方式创建，启动时若表为空则写入演示数据。
- 数据源在 `config/DataSourceConfig` 中按环境变量选择 H2 或 MySQL。
- 前端请求统一封装在 `src/utils/request.js`，开发环境由 Vite 代理转发到后端。
- 业务数据全部走数据库读写，重启后不会丢失（与早期内存存储版本不同）。
