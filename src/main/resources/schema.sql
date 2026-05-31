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
-- user_id=NULL 为系统默认账户，user_id=实际ID 为用户自定义账户
CREATE TABLE IF NOT EXISTS `account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID，NULL为系统默认',
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
    `desc` VARCHAR(255) DEFAULT NULL COMMENT '账户描述',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_type` (`user_id`, `type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账户表';

-- 交易表
CREATE TABLE IF NOT EXISTS `transaction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '交易ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `date` DATE NOT NULL COMMENT '交易日期',
    `time` TIME DEFAULT NULL COMMENT '交易时间',
    `amount` DECIMAL(15, 2) NOT NULL COMMENT '交易金额',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '交易描述',
    `mode` VARCHAR(20) DEFAULT 'simple' COMMENT '记账模式：simple-简化模式, full-完整模式',
    `transaction_type` VARCHAR(20) DEFAULT 'expense' COMMENT '交易类型：expense-支出, income-收入, transfer-转账, repayment-还款',
    `refund_status` VARCHAR(20) DEFAULT 'none' COMMENT '退款状态：none-无退款, partial-部分退款, full-全额退款',
    `refunded_amount` DECIMAL(16, 2) DEFAULT 0.00 COMMENT '已退款金额',
    `net_amount` DECIMAL(16, 2) DEFAULT 0.00 COMMENT '净金额',
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
CREATE TABLE IF NOT EXISTS `entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分录ID',
    `transaction_id` BIGINT NOT NULL COMMENT '关联交易ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID或分类ID',
    `debit` DECIMAL(15, 2) DEFAULT NULL COMMENT '借方金额',
    `credit` DECIMAL(15, 2) DEFAULT NULL COMMENT '贷方金额',
    `account_type` VARCHAR(20) DEFAULT 'account' COMMENT '账户类型：category-分类, account-账户',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_transaction_id` (`transaction_id`),
    CONSTRAINT `fk_entry_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分录表';

-- 分类表
-- user_id=NULL 为系统默认分类，user_id=实际ID 为用户自定义分类
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID，NULL为系统默认',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `type` VARCHAR(20) NOT NULL COMMENT '分类类型：expense-支出, income-收入',
    `icon` VARCHAR(10) DEFAULT '📦' COMMENT '图标emoji',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父分类ID',
    -- `alert_threshold` DECIMAL(5, 2) DEFAULT 80 COMMENT '预警阈值（百分比）',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_type` (`user_id`, `type`)
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
    CONSTRAINT `fk_budget_daily_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常预算表';
-- 专项预算表
CREATE TABLE IF NOT EXISTS `special_budget` (
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
CREATE TABLE IF NOT EXISTS `special_budget_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `special_budget_id` BIGINT NOT NULL COMMENT '专项预算ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_special_budget_id` (`special_budget_id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_sbc_budget` FOREIGN KEY (`special_budget_id`) REFERENCES `special_budget` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sbc_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专项预算关联分类表';

-- 预算结转记录表
CREATE TABLE IF NOT EXISTS `budget_carryover` (
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
    CONSTRAINT `fk_budget_carryover_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算结转记录表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
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
CREATE TABLE IF NOT EXISTS `investment_income` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID',
    `amount` DECIMAL(15, 2) NOT NULL COMMENT '收益金额',
    `date` DATE NOT NULL COMMENT '收益日期',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `type` ENUM('daily','dividend','maturity') NOT NULL DEFAULT 'daily' COMMENT '收益类型',
    `is_reinvested` TINYINT(1) DEFAULT 0 COMMENT '是否再投资',
    `transaction_id` BIGINT DEFAULT NULL COMMENT '关联自动生成的交易ID',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_date` (`date`),
    KEY `idx_user_account_date` (`user_id`, `account_id`, `date`),
    CONSTRAINT `fk_investment_income_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_investment_income_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投资账户收益记录表';

-- 投资账户市值记录表
CREATE TABLE IF NOT EXISTS `investment_value` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `account_id` BIGINT NOT NULL COMMENT '账户ID',
    `market_value` DECIMAL(15, 2) NOT NULL COMMENT '市值',
    `cost_basis` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '成本',
    `gain_rate` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '收益率',
    `unrealized_gain` DECIMAL(15, 2) DEFAULT 0.00 COMMENT '未实现收益',
    `date` DATE NOT NULL COMMENT '记录日期',
    `note` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_date` (`date`),
    KEY `idx_user_account_date` (`user_id`, `account_id`, `date`),
    CONSTRAINT `fk_investment_value_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_investment_value_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投资账户市值记录表';

-- 周期交易模板表
CREATE TABLE IF NOT EXISTS `recurring_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `amount` DECIMAL(15, 2) NOT NULL COMMENT '金额',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：expense-支出, income-收入',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `account_id` BIGINT DEFAULT NULL COMMENT '账户ID',
    `to_account_id` BIGINT DEFAULT NULL COMMENT '目标账户ID',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `frequency` VARCHAR(20) NOT NULL COMMENT '频率：daily-每天, weekly-每周, monthly-每月, yearly-每年',
    `day_of_week` tinyint DEFAULT NULL COMMENT '周几（1-7，weekly专用）',
    `day_of_month` tinyint DEFAULT NULL COMMENT '每月几号（1-31，monthly专用）',
    `month_of_year` tinyint DEFAULT NULL COMMENT '每年几月（1-12，yearly专用）',
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    `max_count` int DEFAULT NULL COMMENT '最大执行次数（可选）',
    `executed_count` INT DEFAULT 0 COMMENT '执行次数',
    `next_trigger_date` DATE DEFAULT NULL COMMENT '下次触发日期',
    `last_triggered_at` DATETIME DEFAULT NULL COMMENT '上次触发时间',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-激活, paused-暂停',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_next_due_date` (`next_due_date`),
    KEY `idx_user_status_due` (`user_id`, `status`, `next_due_date`),
    CONSTRAINT `fk_recurring_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_recurring_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_recurring_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期交易模板表';

-- 交易-标签关联表
CREATE TABLE IF NOT EXISTS `transaction_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `transaction_id` BIGINT NOT NULL COMMENT '交易ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '软删除标记',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_tag_id` (`tag_id`),
    UNIQUE KEY `uk_transaction_tag` (`transaction_id`, `tag_id`),
    CONSTRAINT `fk_transaction_tag_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_transaction_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易标签关联表';

CREATE TABLE refund_record (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '退款ID',
  `transaction_id`       BIGINT        NOT NULL COMMENT '原交易ID',
  `amount`          DECIMAL(16,2) NOT NULL COMMENT '退款金额',
  `date`            DATE          NOT NULL COMMENT '退款日期',
  `description`     VARCHAR(200)  NOT NULL DEFAULT '' COMMENT '退款说明',
  `account_id`      BIGINT        NOT NULL COMMENT '退款入账账户ID',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`      TINYINT(1)      DEFAULT 0 COMMENT '软删除标记',
  PRIMARY KEY (id),
  INDEX idx_transaction (transaction_id),
  CONSTRAINT fk_rr_transaction FOREIGN KEY (transaction_id) REFERENCES transaction(id) ON DELETE CASCADE,
  CONSTRAINT fk_rr_account FOREIGN KEY (account_id) REFERENCES account(id)
) ENGINE=InnoDB COMMENT='退款记录表';

-- 用户数据隐藏表（用户隐藏系统默认的分类/账户时写入）
CREATE TABLE IF NOT EXISTS `user_data_hide` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `data_type`  VARCHAR(10)  NOT NULL COMMENT '数据类型：category / account',
    `ref_id`     BIGINT       NOT NULL COMMENT '被隐藏的系统默认数据ID',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_ref` (`user_id`, `data_type`, `ref_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户数据隐藏表';

-- ============================================================
-- 系统默认数据初始化（user_id=NULL 表示属于系统）
-- ============================================================

-- 默认账户（共10个）
INSERT INTO account (user_id, name, type, icon, color, initial_balance, include_in_net_worth, allow_transfer, sort_order, is_deleted, created_at, updated_at) VALUES
(NULL, '现金',         'asset',      '💵', '#07C160', 0, 1, 1, 1, 0, NOW(), NOW()),
(NULL, '支付宝余额',   'asset',      '💙', '#1677FF', 0, 1, 1, 2, 0, NOW(), NOW()),
(NULL, '微信钱包',     'asset',      '💚', '#07C160', 0, 1, 1, 3, 0, NOW(), NOW()),
(NULL, '银行卡',       'asset',      '🏦', '#FF6B6B', 0, 1, 1, 4, 0, NOW(), NOW()),
(NULL, '信用卡',       'liability',  '💳', '#4ECDC4', 0, 1, 1, 1, 0, NOW(), NOW()),
(NULL, '花呗',         'liability',  '🌸', '#FF85A2', 0, 1, 1, 2, 0, NOW(), NOW()),
(NULL, '借款',         'liability',  '🤝', '#C9B1FF', 0, 1, 1, 3, 0, NOW(), NOW()),
(NULL, '股票账户',     'investment', '📊', '#E17055', 0, 1, 1, 1, 0, NOW(), NOW()),
(NULL, '基金账户',     'investment', '📈', '#00B894', 0, 1, 1, 2, 0, NOW(), NOW()),
(NULL, '定期存款',     'investment', '🏛️', '#6C5CE7', 0, 1, 1, 3, 0, NOW(), NOW());

-- 默认支出大类（共13个）
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at) VALUES
(NULL, '餐饮',       'expense', '🍜',  NULL, 1,  0, NOW()),
(NULL, '交通',       'expense', '🚗',  NULL, 2,  0, NOW()),
(NULL, '购物',       'expense', '🛒',  NULL, 3,  0, NOW()),
(NULL, '居住',       'expense', '🏠',  NULL, 4,  0, NOW()),
(NULL, '医疗',       'expense', '🏥',  NULL, 5,  0, NOW()),
(NULL, '育儿',       'expense', '👶',  NULL, 6,  0, NOW()),
(NULL, '娱乐',       'expense', '🎬',  NULL, 7,  0, NOW()),
(NULL, '教育',       'expense', '📚',  NULL, 8,  0, NOW()),
(NULL, '通讯',       'expense', '📱',  NULL, 9,  0, NOW()),
(NULL, '人情',       'expense', '👫',  NULL, 10, 0, NOW()),
(NULL, '宠物',       'expense', '🐱',  NULL, 11, 0, NOW()),
(NULL, '金融保险',   'expense', '🏛️', NULL, 12, 0, NOW()),
(NULL, '其他支出',   'expense', '🔧',  NULL, 99, 0, NOW());

-- 默认支出二级分类（通过名称动态查找 parent_id）
-- 餐饮子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '早午晚餐', 'expense', '🍚', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='餐饮' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '零食水果', 'expense', '🍎', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='餐饮' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '外卖',     'expense', '🥡', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='餐饮' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '饮品咖啡', 'expense', '☕', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='餐饮' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '聚餐宴请', 'expense', '🎉', id, 5, 0, NOW() FROM category WHERE user_id IS NULL AND name='餐饮' AND type='expense';
-- 交通子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '公交地铁', 'expense', '🚇', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='交通' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '打车拼车', 'expense', '🚕', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='交通' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '加油充电', 'expense', '⛽', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='交通' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '停车费',   'expense', '🅿️', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='交通' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '火车飞机', 'expense', '✈️', id, 5, 0, NOW() FROM category WHERE user_id IS NULL AND name='交通' AND type='expense';
-- 购物子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '服饰鞋包', 'expense', '👗', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='购物' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '数码电子', 'expense', '📱', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='购物' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '家居日用', 'expense', '🏠', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='购物' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '个人护理', 'expense', '💄', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='购物' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '其他购物', 'expense', '🛍️', id, 5, 0, NOW() FROM category WHERE user_id IS NULL AND name='购物' AND type='expense';
-- 居住子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '房租',     'expense', '🏢', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='居住' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '房贷',     'expense', '🏦', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='居住' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '水电燃气', 'expense', '💡', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='居住' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '物业费',   'expense', '🏘️', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='居住' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '维修装修', 'expense', '🔧', id, 5, 0, NOW() FROM category WHERE user_id IS NULL AND name='居住' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '宽带网络', 'expense', '🌐', id, 6, 0, NOW() FROM category WHERE user_id IS NULL AND name='居住' AND type='expense';
-- 医疗子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '门诊', 'expense', '🏥', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='医疗' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '住院', 'expense', '🏨', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='医疗' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '药品', 'expense', '💊', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='医疗' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '体检', 'expense', '🩺', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='医疗' AND type='expense';
-- 育儿子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '奶粉尿布', 'expense', '🍼', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='育儿' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '教育学费', 'expense', '🏫', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='育儿' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '玩具',     'expense', '🧸', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='育儿' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '兴趣班',   'expense', '🎨', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='育儿' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '其他育儿', 'expense', '👶', id, 5, 0, NOW() FROM category WHERE user_id IS NULL AND name='育儿' AND type='expense';
-- 娱乐子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, 'KTV电影',  'expense', '🎬', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='娱乐' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '旅行度假', 'expense', '✈️', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='娱乐' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '游戏',     'expense', '🎮', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='娱乐' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '运动健身', 'expense', '🏃', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='娱乐' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '其他娱乐', 'expense', '🎉', id, 5, 0, NOW() FROM category WHERE user_id IS NULL AND name='娱乐' AND type='expense';
-- 教育子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '书籍',     'expense', '📖', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='教育' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '在线课程', 'expense', '💻', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='教育' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '文具',     'expense', '✏️', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='教育' AND type='expense';
-- 通讯子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '手机话费', 'expense', '📞', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='通讯' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '快递邮寄', 'expense', '📦', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='通讯' AND type='expense';
-- 人情子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '红包礼金', 'expense', '🧧', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='人情' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '请客送礼', 'expense', '🎁', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='人情' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '孝敬长辈', 'expense', '👴', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='人情' AND type='expense';
-- 宠物子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '宠物食品', 'expense', '🐕', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='宠物' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '宠物医疗', 'expense', '🏥', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='宠物' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '宠物用品', 'expense', '🧹', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='宠物' AND type='expense';
-- 金融保险子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '保险',     'expense', '🛡️', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='金融保险' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '贷款利息', 'expense', '💰', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='金融保险' AND type='expense';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '手续费',   'expense', '💳', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='金融保险' AND type='expense';
-- 其他支出子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '其他支出', 'expense', '🔧', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='其他支出' AND type='expense';

-- 默认收入大类（共6个）
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at) VALUES
(NULL, '工资',       'income', '💰', NULL, 1,  0, NOW()),
(NULL, '兼职副业',   'income', '💵', NULL, 2,  0, NOW()),
(NULL, '投资收益',   'income', '📈', NULL, 3,  0, NOW()),
(NULL, '红包礼金',   'income', '🎁', NULL, 4,  0, NOW()),
(NULL, '退款报销',   'income', '🔙', NULL, 5,  0, NOW()),
(NULL, '其他收入',   'income', '🔧', NULL, 99, 0, NOW());

-- 默认收入二级分类（按parent_id关联，共约19个）
-- 工资子类 (parent_id for 工资 after insert)
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '基本工资', 'income', '💰', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='工资' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '加班费',   'income', '🕐', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='工资' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '奖金年终', 'income', '🎉', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='工资' AND type='income';
-- 兼职副业子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '副业兼职', 'income', '💻', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='兼职副业' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '自由职业', 'income', '✍️', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='兼职副业' AND type='income';
-- 投资收益子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '理财收益', 'income', '📊', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='投资收益' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '股息红利', 'income', '💹', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='投资收益' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '房租收入', 'income', '🏠', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='投资收益' AND type='income';
-- 红包礼金子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '微信红包', 'income', '🧧', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='红包礼金' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '节日礼金', 'income', '🎁', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='红包礼金' AND type='income';
-- 退款报销子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '购物退款',   'income', '🛍️', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='退款报销' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '差旅报销',   'income', '🚗', id, 2, 0, NOW() FROM category WHERE user_id IS NULL AND name='退款报销' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '餐补交通补', 'income', '🍱', id, 3, 0, NOW() FROM category WHERE user_id IS NULL AND name='退款报销' AND type='income';
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '其他退款',   'income', '🔙', id, 4, 0, NOW() FROM category WHERE user_id IS NULL AND name='退款报销' AND type='income';
-- 其他收入子类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, is_deleted, created_at)
SELECT NULL, '其他收入', 'income', '🔧', id, 1, 0, NOW() FROM category WHERE user_id IS NULL AND name='其他收入' AND type='income';
