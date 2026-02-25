package com.xdud.seckillbot.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public enum PlatformType implements IEnum<String> {
    IMOUTAI("imoutai", "i茅台"),
    DAMAI("damai", "大麦网"),
    MAOYAN("maoyan", "猫眼");

    private final String value;
    private final String description;

    PlatformType(String value, String description) {
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
