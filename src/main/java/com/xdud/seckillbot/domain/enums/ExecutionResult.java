package com.xdud.seckillbot.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ExecutionResult implements IEnum<String> {
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    NO_STOCK("no_stock", "无库存"),
    AUTH_EXPIRED("auth_expired", "认证过期"),
    NETWORK_ERROR("network_error", "网络错误"),
    TIMEOUT("timeout", "超时"),
    SKIPPED("skipped", "已跳过");

    private final String value;
    private final String description;

    ExecutionResult(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
