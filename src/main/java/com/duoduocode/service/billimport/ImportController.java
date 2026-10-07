package com.duoduocode.service.billimport;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账单导入控制器
 * parse 无状态预览 / confirm 确认落库
 */
@Slf4j
@RestController
@RequestMapping("/v1/imports")
@RequiredArgsConstructor
@Tag(name = "账单导入", description = "支付宝/微信账单 CSV 导入（解析预览与确认落库）")
public class ImportController {

    private final ImportService importService;

    /**
     * 导入预览（无状态：不落库不建账户）
     * POST /v1/imports/parse
     */
    @PostMapping("/parse")
    @Operation(summary = "导入预览", description = "解析账单文件并返回行明细、判重结果与将创建的账户清单，不产生任何写入")
    public Result<ImportParseVO> parse(
            @Parameter(description = "文件名与 base64 内容") @RequestBody ImportParseDTO dto) {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/imports/parse - userId={}, fileName={}", userId, dto == null ? null : dto.getFileName());
        return Result.success(importService.parse(userId, dto));
    }

    /**
     * 确认导入（整批单事务：重解析→逐行判重→建账→落库→批次记录）
     * R003：可导入行 ≥50 转异步任务，返回 taskId 由前端轮询进度
     * POST /v1/imports/confirm
     */
    @PostMapping("/confirm")
    @Operation(summary = "确认导入", description = "整批一个事务落库（≥50 笔转异步任务）；单号重复行默认跳过，allowDuplicates=true 时强制导入")
    public Result<ImportConfirmVO> confirm(
            @Parameter(description = "文件名、base64 内容与是否允许重复") @RequestBody ImportConfirmDTO dto) {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/imports/confirm - userId={}, fileName={}, allowDuplicates={}",
                userId, dto == null ? null : dto.getFileName(), dto == null ? null : dto.getAllowDuplicates());
        return Result.success(importService.confirmOrDispatch(userId, dto));
    }
}
