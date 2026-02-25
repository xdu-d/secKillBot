package com.xdud.seckillbot.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xdud.seckillbot.domain.enums.PlatformType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("platform")
public class Platform {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("platform_type")
    private PlatformType platformType;

    @TableField("name")
    private String name;

    @TableField("base_url")
    private String baseUrl;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("config_json")
    private String configJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
