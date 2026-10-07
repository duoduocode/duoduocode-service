-- ============================================================
-- R004 增量脚本：订阅视图 + 月度报告 + 健康度（月报快照表）
-- 存量库执行；幂等可重跑（CREATE IF NOT EXISTS）
-- 对应设计：requirements/R004-subscription-report-health/05-技术设计.md §2
-- ============================================================

-- 1. 月度财务报告快照表（新建；核心指标列 + detail JSON，S4-Q2 裁决）
CREATE TABLE IF NOT EXISTS `monthly_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `month` VARCHAR(7) NOT NULL COMMENT '报告月份 YYYY-MM',
    `total_income` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '月总收入',
    `total_expense` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '月总支出',
    `savings_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '储蓄率%，收入为0时NULL',
    `net_worth_start` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期初净资产(上月1日)',
    `net_worth_end` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '期末净资产(上月末)',
    `subscription_spend` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '报告月订阅扣款合计',
    `detail` TEXT COMMENT '归因/预算执行/分类top快照JSON',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_month` (`user_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度财务报告快照';
