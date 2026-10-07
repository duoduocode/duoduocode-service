-- ============================================================
-- R002 增量脚本：站内消息中心 + CSV 导入导出
-- 存量库执行；幂等可重跑（列存在检测 / CREATE IF NOT EXISTS）
-- 对应设计：requirements/R002-subscribe-message-csv/05-技术设计.md §2
-- ============================================================

-- 1. 站内消息/待办表（新建）
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `msg_type` VARCHAR(10) NOT NULL COMMENT 'todo-待办, notice-通知',
    `type` VARCHAR(30) NOT NULL COMMENT '业务类型：recurring_due/budget_warning/budget_exceeded/repayment_due',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    `ref_type` VARCHAR(20) DEFAULT NULL COMMENT '关联对象：template/category/account',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联对象ID',
    `dedup_key` VARCHAR(100) NOT NULL COMMENT '幂等键',
    `status` VARCHAR(20) NOT NULL DEFAULT 'unread' COMMENT 'unread/read/digested',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `read_at` DATETIME DEFAULT NULL,
    `digested_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_dedup` (`user_id`, `dedup_key`),
    KEY `idx_user_status_time` (`user_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息/待办';

-- 2. 账单导入批次表（新建，日志性质，不做软删）
CREATE TABLE IF NOT EXISTS `import_batch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `source` VARCHAR(20) NOT NULL COMMENT 'alipay/wechat',
    `file_name` VARCHAR(200) DEFAULT NULL,
    `total_rows` INT NOT NULL DEFAULT 0,
    `imported_count` INT NOT NULL DEFAULT 0,
    `skipped_count` INT NOT NULL DEFAULT 0 COMMENT '判重跳过',
    `ignored_count` INT NOT NULL DEFAULT 0 COMMENT '不计收支/非法行',
    `created_accounts` VARCHAR(500) DEFAULT NULL COMMENT '自动创建的账户名,逗号分隔',
    `status` VARCHAR(20) NOT NULL DEFAULT 'confirmed' COMMENT 'confirmed/failed',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单导入批次';

-- 3. transaction 表：导入/待办溯源三列（不加唯一约束，「允许重复行仍导入」需放过重复单号，判重靠查询）
-- 幂等：通过 information_schema 判断列是否存在再执行 ALTER
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction' AND COLUMN_NAME = 'source_template_id') = 0,
    'ALTER TABLE `transaction` ADD COLUMN `source_template_id` BIGINT DEFAULT NULL COMMENT ''来源周期模板ID（待办闭环）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction' AND COLUMN_NAME = 'external_order_no') = 0,
    'ALTER TABLE `transaction` ADD COLUMN `external_order_no` VARCHAR(64) DEFAULT NULL COMMENT ''导入来源账单交易单号''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction' AND COLUMN_NAME = 'import_batch_id') = 0,
    'ALTER TABLE `transaction` ADD COLUMN `import_batch_id` BIGINT DEFAULT NULL COMMENT ''导入批次ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transaction' AND INDEX_NAME = 'idx_user_order') = 0,
    'ALTER TABLE `transaction` ADD INDEX `idx_user_order` (`user_id`, `external_order_no`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. account 表：还款日提醒数据源（可空；仅负债账户参与扫描）
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'account' AND COLUMN_NAME = 'bill_day') = 0,
    'ALTER TABLE `account` ADD COLUMN `bill_day` TINYINT DEFAULT NULL COMMENT ''账单日 1-31''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'account' AND COLUMN_NAME = 'repayment_day') = 0,
    'ALTER TABLE `account` ADD COLUMN `repayment_day` TINYINT DEFAULT NULL COMMENT ''还款日 1-31''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
