package com.duoduocode.service.export;

import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 数据导出控制器
 * CSV 账单导出 / JSON 全量备份
 */
@Slf4j
@RestController
@RequestMapping("/v1/exports")
@RequiredArgsConstructor
@Tag(name = "数据导出", description = "账单 CSV 导出（UTF-8 BOM，含借/贷双列）与全量 JSON 备份")
public class ExportController {

    private final ExportService exportService;

    /**
     * 导出 CSV（全部未删除交易）
     * GET /v1/exports/csv
     */
    @GetMapping("/csv")
    @Operation(summary = "导出 CSV", description = "全部未删除交易导出为 UTF-8 BOM CSV，列：日期,时间,类型,分类,金额,账户,备注,借方,贷方,来源单号")
    public ResponseEntity<byte[]> exportCsv() {
        Long userId = SecurityContext.requireUserId();
        log.info("GET /v1/exports/csv - userId={}", userId);
        return fileResponse(exportService.exportCsv(userId),
                ExportService.buildFileName("csv", LocalDate.now()), "text/csv; charset=utf-8");
    }

    /**
     * 导出 JSON（全量备份：账户/分类/交易含分录/预算，交易含软删除标记）
     * GET /v1/exports/json
     */
    @GetMapping("/json")
    @Operation(summary = "导出 JSON", description = "全量备份：accounts/categories/transactions(含 entries)/budgets 五键，version=1")
    public ResponseEntity<byte[]> exportJson() {
        Long userId = SecurityContext.requireUserId();
        log.info("GET /v1/exports/json - userId={}", userId);
        return fileResponse(exportService.exportJson(userId),
                ExportService.buildFileName("json", LocalDate.now()), "application/json; charset=utf-8");
    }

    private ResponseEntity<byte[]> fileResponse(byte[] body, String fileName, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
