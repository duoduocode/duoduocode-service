-- 创建数据库
CREATE DATABASE IF NOT EXISTS duoduocode DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE duoduocode;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid` VARCHAR(64) NOT NULL COMMENT '微信小程序用户唯一标识',
    `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信开放平台唯一标识',
    `session_key` VARCHAR(128) DEFAULT NULL COMMENT '微信会话密钥',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '用户头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `gender` TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_phone` (`phone`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 账户表
CREATE TABLE IF NOT EXISTS `accounts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '账户名称',
    `type` VARCHAR(20) NOT NULL COMMENT '账户类型：asset-资产, liability-负债, investment-投资',
    `icon` VARCHAR(10) DEFAULT '💰' COMMENT '图标emoji',
    `color` VARCHAR(20) DEFAULT '#07C160' COMMENT '颜色',
    `initial_balance` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '初始余额',
    `credit_limit` DECIMAL(15, 2) DEFAULT NULL COMMENT '信用额度（信用卡用）',
    `include_in_net_worth` TINYINT(1) DEFAULT 1 COMMENT '是否计入净资产',
    `allow_transfer` TINYINT(1) DEFAULT 1 COMMENT '是否允许转账',
    `enable_alert` TINYINT(1) DEFAULT 0 COMMENT '是否启用余额预警',
    `alert_threshold` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '预警阈值',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_type` (`user_id`, `type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- 交易表
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '交易ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `date` DATE NOT NULL COMMENT '交易日期',
    `time` TIME DEFAULT NULL COMMENT '交易时间',
    `amount` DECIMAL(15, 2) NOT NULL COMMENT '交易金额',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '交易描述',
    `mode` VARCHAR(20) DEFAULT 'simple' COMMENT '记账模式：simple-简化模式, full-完整模式',
    `transaction_type` VARCHAR(20) DEFAULT 'expense' COMMENT '交易类型：expense-支出, income-收入, transfer-转账, repayment-还款',
    `refund_status` VARCHAR(20) DEFAULT 'none' COMMENT '退款状态：none-无退款, partial-部分退款, full-全额退款',
    `refunded_amount` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '已退款金额',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_date` (`date`),
    KEY `idx_user_date` (`user_id`, `date`),
    KEY `idx_refund_status` (`refund_status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易表';

-- 分录表（复式记账核心）
CREATE TABLE IF NOT EXISTS `entries` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分录ID',
    `transaction_id` BIGINT NOT NULL COMMENT '关联交易ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID或分类ID',
    `debit` DECIMAL(15, 2) DEFAULT NULL COMMENT '借方金额',
    `credit` DECIMAL(15, 2) DEFAULT NULL COMMENT '贷方金额',
    `account_type` VARCHAR(20) DEFAULT 'account' COMMENT '账户类型：category-分类, account-账户',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_account_type` (`account_type`),
    CONSTRAINT `fk_entry_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分录表';

-- 分类表
CREATE TABLE IF NOT EXISTS `categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `type` VARCHAR(20) NOT NULL COMMENT '分类类型：expense-支出, income-收入',
    `icon` VARCHAR(10) DEFAULT '📦' COMMENT '图标emoji',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
    `monthly_budget` DECIMAL(15, 2) DEFAULT NULL COMMENT '月度预算',
    `weekly_budget` DECIMAL(15, 2) DEFAULT NULL COMMENT '周预算',
    `alert_threshold` DECIMAL(5, 2) DEFAULT 80 COMMENT '预警阈值（百分比）',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_type` (`user_id`, `type`),
    CONSTRAINT `fk_category_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 日常预算表
CREATE TABLE IF NOT EXISTS `budget_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预算ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `month` VARCHAR(7) NOT NULL COMMENT '月份 YYYY-MM',
    `monthly_budget` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '月度预算',
    `weekly_budget` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '周预算',
    `alert_threshold` DECIMAL(5, 2) DEFAULT 80 COMMENT '预警阈值（百分比）',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_category_month` (`user_id`, `category_id`, `month`),
    KEY `idx_month` (`month`),
    CONSTRAINT `fk_budget_daily_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_budget_daily_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常预算表';

-- 专项预算表
CREATE TABLE IF NOT EXISTS `special_budgets` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预算ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '预算名称',
    `total_amount` DECIMAL(15, 2) NOT NULL COMMENT '预算总金额',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `status` VARCHAR(20) DEFAULT 'ongoing' COMMENT '状态：ongoing-进行中, completed-已结束',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `actual_amount` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '实际支出金额',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_date_range` (`start_date`, `end_date`),
    CONSTRAINT `fk_special_budget_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专项预算表';

-- 专项预算关联分类表
CREATE TABLE IF NOT EXISTS `special_budget_categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `special_budget_id` BIGINT NOT NULL COMMENT '专项预算ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_special_budget_id` (`special_budget_id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_sbc_budget` FOREIGN KEY (`special_budget_id`) REFERENCES `special_budgets` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sbc_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专项预算关联分类表';

-- 预算结转记录表
CREATE TABLE IF NOT EXISTS `budget_carryovers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '结转ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `from_month` VARCHAR(7) NOT NULL COMMENT '来源月份 YYYY-MM',
    `to_month` VARCHAR(7) NOT NULL COMMENT '目标月份 YYYY-MM',
    `carryover_amount` DECIMAL(15, 2) NOT NULL COMMENT '结转金额',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_to_month` (`to_month`),
    KEY `idx_category_to_month` (`category_id`, `to_month`),
    CONSTRAINT `fk_budget_carryover_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_budget_carryover_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算结转记录表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `color` VARCHAR(10) DEFAULT '#07C160' COMMENT '颜色（十六进制）',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_tag_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 投资账户收益记录表
CREATE TABLE IF NOT EXISTS `investment_incomes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID',
    `amount` DECIMAL(15, 2) NOT NULL COMMENT '收益金额',
    `date` DATE NOT NULL COMMENT '收益日期',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `reinvested` TINYINT(1) DEFAULT 0 COMMENT '是否再投资',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_date` (`date`),
    KEY `idx_user_account_date` (`user_id`, `account_id`, `date`),
    CONSTRAINT `fk_investment_income_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_investment_income_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投资账户收益记录表';

-- 投资账户市值记录表
CREATE TABLE IF NOT EXISTS `investment_values` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID',
    `market_value` DECIMAL(15, 2) NOT NULL COMMENT '市值',
    `cost_basis` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '成本',
    `date` DATE NOT NULL COMMENT '记录日期',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_date` (`date`),
    KEY `idx_user_account_date` (`user_id`, `account_id`, `date`),
    CONSTRAINT `fk_investment_value_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_investment_value_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投资账户市值记录表';

-- 周期交易模板表
CREATE TABLE IF NOT EXISTS `recurring_templates` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `amount` DECIMAL(15, 2) NOT NULL COMMENT '金额',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：expense-支出, income-收入',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `account_id` BIGINT DEFAULT NULL COMMENT '账户ID',
    `frequency` VARCHAR(20) NOT NULL COMMENT '频率：daily-每天, weekly-每周, monthly-每月, yearly-每年',
    `next_due_date` DATE DEFAULT NULL COMMENT '下次执行日期',
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-激活, paused-暂停',
    `last_triggered_at` DATETIME DEFAULT NULL COMMENT '上次触发时间',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_next_due_date` (`next_due_date`),
    KEY `idx_user_status_due` (`user_id`, `status`, `next_due_date`),
    CONSTRAINT `fk_recurring_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_recurring_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_recurring_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期交易模板表';

-- 交易-标签关联表
CREATE TABLE IF NOT EXISTS `transaction_tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `transaction_id` BIGINT NOT NULL COMMENT '交易ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_tag_id` (`tag_id`),
    UNIQUE KEY `uk_transaction_tag` (`transaction_id`, `tag_id`),
    CONSTRAINT `fk_transaction_tag_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_transaction_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易标签关联表';
