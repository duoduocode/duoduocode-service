package com.duoduocode.service.billimport;

/**
 * 导入确认请求（confirm 时重传重解析，不依赖服务端暂存）
 */
public class ImportConfirmDTO extends ImportParseDTO {

    /**
     * true 时重复单号行也落库
     */
    private Boolean allowDuplicates;

    public Boolean getAllowDuplicates() {
        return allowDuplicates;
    }

    public void setAllowDuplicates(Boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }
}
