package com.duoduocode.service.common;

import lombok.Getter;

/**
 * 响应错误码枚举
 *
 * 错误码规范：
 * - 0: 成功
 * - 1xxxx: 通用错误
 * - 2xxxx: 认证授权错误
 * - 3xxxx: 业务逻辑错误
 * - 5xxxx: 服务端错误
 *
 * @author duoduo
 */
@Getter
public enum ResultCode {

    // ==================== 成功 ====================
    SUCCESS(0, "success"),

    // ==================== 通用错误 1xxxx ====================
    FAIL(10000, "操作失败"),
    PARAM_ERROR(10001, "参数错误"),
    PARAM_MISSING(10002, "参数缺失"),
    PARAM_FORMAT_ERROR(10003, "参数格式错误"),
    REQUEST_METHOD_ERROR(10004, "请求方法错误"),
    REQUEST_FREQUENCY_LIMIT(10005, "请求频率超限"),
    DATA_NOT_FOUND(10006, "数据不存在"),
    DATA_ALREADY_EXISTS(10007, "数据已存在"),
    DATA_DUPLICATE(10008, "数据重复"),
    FILE_UPLOAD_ERROR(10009, "文件上传失败"),
    FILE_SIZE_EXCEEDED(10010, "文件大小超限"),
    FILE_TYPE_ERROR(10011, "文件类型错误"),

    // ==================== 认证授权错误 2xxxx ====================
    UNAUTHORIZED(20000, "未登录或登录已过期"),
    TOKEN_INVALID(20001, "Token无效"),
    TOKEN_EXPIRED(20002, "Token已过期"),
    TOKEN_MISSING(20003, "Token缺失"),
    LOGIN_ERROR(20004, "登录失败"),
    ACCOUNT_NOT_FOUND(20005, "账号不存在"),
    PASSWORD_ERROR(20006, "密码错误"),
    ACCOUNT_DISABLED(20007, "账号已被禁用"),
    ACCOUNT_LOCKED(20008, "账号已被锁定"),
    PERMISSION_DENIED(20009, "权限不足"),
    ACCESS_DENIED(20010, "拒绝访问"),
    CAPTCHA_ERROR(20011, "验证码错误"),
    CAPTCHA_EXPIRED(20012, "验证码已过期"),

    // ==================== 业务逻辑错误 3xxxx ====================
    BUSINESS_ERROR(30000, "业务处理失败"),
    USER_NOT_FOUND(30001, "用户不存在"),
    USER_ALREADY_EXISTS(30002, "用户已存在"),
    USER_PHONE_EXISTS(30003, "手机号已被注册"),
    USER_EMAIL_EXISTS(30004, "邮箱已被注册"),
    ORDER_NOT_FOUND(30010, "订单不存在"),
    ORDER_STATUS_ERROR(30011, "订单状态错误"),
    ORDER_ALREADY_PAID(30012, "订单已支付"),
    ORDER_ALREADY_CANCELLED(30013, "订单已取消"),
    PRODUCT_NOT_FOUND(30020, "商品不存在"),
    PRODUCT_STOCK_NOT_ENOUGH(30021, "商品库存不足"),
    PRODUCT_OFF_SHELF(30022, "商品已下架"),
    PAY_ERROR(30030, "支付失败"),
    REFUND_ERROR(30031, "退款失败"),

    // ==================== 服务端错误 5xxxx ====================
    INTERNAL_ERROR(50000, "服务器内部错误"),
    DATABASE_ERROR(50001, "数据库操作失败"),
    CACHE_ERROR(50002, "缓存操作失败"),
    NETWORK_ERROR(50003, "网络连接失败"),
    SERVICE_UNAVAILABLE(50004, "服务暂不可用"),
    THIRD_PARTY_ERROR(50005, "第三方服务异常"),
    CONFIG_ERROR(50006, "配置错误");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
