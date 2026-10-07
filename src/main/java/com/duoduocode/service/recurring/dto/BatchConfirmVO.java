package com.duoduocode.service.recurring.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量确认/跳过聚合结果
 */
@Data
public class BatchConfirmVO {

    /** sync/async */
    private String mode;

    private Long taskId;

    private int successCount;

    private int failCount;

    private List<Failure> failures = new ArrayList<>();

    public static BatchConfirmVO summarize(List<ItemResult> results) {
        BatchConfirmVO vo = new BatchConfirmVO();
        for (ItemResult r : results) {
            if (r.isSuccess()) {
                vo.successCount++;
            } else {
                vo.failCount++;
                vo.failures.add(new Failure(r.getId(), r.getReason()));
            }
        }
        return vo;
    }

    @Data
    public static class Failure {

        private Long id;

        private String reason;

        public Failure() {
        }

        public Failure(Long id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }
}
