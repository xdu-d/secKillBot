package com.xdud.seckillbot.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误 1xxx
    TASK_NOT_FOUND(1001, "任务不存在"),
    TASK_STATUS_CONFLICT(1002, "任务状态不允许该操作"),
    ACCOUNT_NOT_FOUND(1003, "账号不存在"),
    ACCOUNT_AUTH_EXPIRED(1004, "账号认证已过期，请重新登录"),
    PLATFORM_NOT_SUPPORTED(1005, "平台暂不支持"),
    NO_STOCK(1006, "商品已无库存"),
    ORDER_FAILED(1007, "下单失败"),

    // 安全错误 2xxx
    TOKEN_INVALID(2001, "Token 无效"),
    TOKEN_EXPIRED(2002, "Token 已过期"),
    PASSWORD_WRONG(2003, "用户名或密码错误"),
    USER_DISABLED(2004, "用户已被禁用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
