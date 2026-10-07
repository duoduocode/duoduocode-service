package com.duoduocode.service.budget.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * R003 批量结转聚合结果
 */
@Data
public class BatchCarryoverVO {

    private int successCount;

    private int failCount;

    private List<Failure> failures = new ArrayList<>();

    @Data
    public static class Failure {

        private Long categoryId;

        private String reason;

        public Failure() {
        }

        public Failure(Long categoryId, String reason) {
            this.categoryId = categoryId;
            this.reason = reason;
        }
    }
}
