package com.xdud.seckillbot.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum TaskStatus implements IEnum<String> {
    DRAFT("draft", "草稿"),
    SCHEDULED("scheduled", "已调度"),
    RUNNING("running", "执行中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消");

    private final String value;
    private final String description;

    TaskStatus(String value, String description) {
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
