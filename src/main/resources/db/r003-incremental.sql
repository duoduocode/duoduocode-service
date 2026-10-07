-- ============================================================
-- R003 增量脚本：业务流程优化 7 项（批量誊录异步任务）
-- 存量库执行；幂等可重跑（CREATE IF NOT EXISTS）
-- 对应设计：requirements/R003-process-optimization/05-技术设计.md §2
-- ============================================================

-- 1. 批量誊录异步任务表（新建，日志性质，不做软删）
CREATE TABLE IF NOT EXISTS `recalculation_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `task_type` VARCHAR(20) NOT NULL COMMENT 'batch_confirm/import',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/succeeded/failed',
    `total_count` INT NOT NULL DEFAULT 0,
    `success_count` INT NOT NULL DEFAULT 0,
    `fail_count` INT NOT NULL DEFAULT 0,
    `failure_detail` TEXT DEFAULT NULL COMMENT '失败明细JSON [{id,reason}]',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '任务级异常',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联对象（import=import_batch_id）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `started_at` DATETIME DEFAULT NULL,
    `finished_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_type_created` (`user_id`, `task_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量誊录异步任务';
