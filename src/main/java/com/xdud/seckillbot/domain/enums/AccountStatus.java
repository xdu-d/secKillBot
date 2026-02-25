package com.xdud.seckillbot.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum AccountStatus implements IEnum<String> {
    ACTIVE("active", "正常"),
    EXPIRED("expired", "认证过期"),
    BANNED("banned", "封禁"),
    DISABLED("disabled", "已禁用");

    private final String value;
    private final String description;

    AccountStatus(String value, String description) {
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
