package com.duoduocode.service.recurring.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量确认/跳过请求
 */
@Data
public class BatchOperateDTO {

    private List<Long> templateIds;
}
