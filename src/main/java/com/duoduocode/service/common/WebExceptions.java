package com.duoduocode.service.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 业务异常 → HTTP 状态映射（R003 新端点使用；存量端点保持 200+code 契约不变）
 */
public final class WebExceptions {

    private WebExceptions() {
    }

    public static RuntimeException fromBusiness(BusinessException e) {
        HttpStatus status = map(e.getCode());
        if (status == null) {
            return e;
        }
        return new ResponseStatusException(status, e.getMessage());
    }

    private static HttpStatus map(Integer code) {
        if (code == null) {
            return null;
        }
        switch (code) {
            case 10001: // PARAM_ERROR
            case 10007: // DATA_ALREADY_EXISTS
                return HttpStatus.BAD_REQUEST;
            case 10006: // DATA_NOT_FOUND
                return HttpStatus.NOT_FOUND;
            case 20009: // PERMISSION_DENIED
                return HttpStatus.FORBIDDEN;
            default:
                return null;
        }
    }
}
