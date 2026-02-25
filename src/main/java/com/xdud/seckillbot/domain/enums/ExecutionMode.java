package com.xdud.seckillbot.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum ExecutionMode implements IEnum<String> {
    PARALLEL("parallel", "并行"),
    SEQUENTIAL("sequential", "顺序");

    private final String value;
    private final String description;

    ExecutionMode(String value, String description) {
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
