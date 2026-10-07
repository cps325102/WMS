-- ============================================================================
-- WMS —— MySQL 初始化脚本（建库 + 建专用账号）
--
-- 用途：用 root 执行一次，创建 wms 库和应用专用账号。
--       表结构与种子数据不在这里创建，由应用启动时执行
--       backend/src/main/resources/schema.sql 与 data.sql 自动完成。
--
-- 执行方式（二选一）：
--   mysql -u root -p < db/setup.sql
--   或在 MySQL Workbench / Navicat 里用 root 连接后整体执行
--
-- 执行完成后，用下面的 wms 账号即可连接查看数据库：
--   主机 localhost   端口 3306   数据库 wms
--   用户名 wms       密码 Wms@2026
-- ============================================================================

CREATE DATABASE IF NOT EXISTS wms
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 应用专用账号：只授权 wms 库，避免应用用 root 连接。
-- 密码可用环境变量 WMS_DB_PASSWORD 覆盖，改这里的话记得同步改。
CREATE USER IF NOT EXISTS 'wms'@'localhost' IDENTIFIED BY 'Wms@2026';
CREATE USER IF NOT EXISTS 'wms'@'127.0.0.1' IDENTIFIED BY 'Wms@2026';

-- 建表脚本随应用启动执行，因此需要 CREATE / ALTER / INDEX / REFERENCES 权限
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES,
      CREATE VIEW, SHOW VIEW
    ON wms.* TO 'wms'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES,
      CREATE VIEW, SHOW VIEW
    ON wms.* TO 'wms'@'127.0.0.1';

FLUSH PRIVILEGES;

-- 查看结果：
--   SHOW DATABASES;
--   SELECT user, host FROM mysql.user WHERE user = 'wms';
--   SHOW GRANTS FOR 'wms'@'localhost';
