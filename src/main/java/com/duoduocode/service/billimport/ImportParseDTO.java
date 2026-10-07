package com.duoduocode.service.billimport;

/**
 * 导入解析请求
 */
public class ImportParseDTO {

    private String fileName;

    private String contentBase64;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }
}
