package com.xdud.seckillbot.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xdud.seckillbot.domain.enums.ExecutionMode;
import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.domain.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("platform_type")
    private PlatformType platformType;

    @TableField("product_id")
    private String productId;

    @TableField("product_name")
    private String productName;

    /** 额外的商品参数（场次/SKU等），JSON 格式 */
    @TableField("product_params")
    private String productParams;

    /** 精确到毫秒的触发时间 */
    @TableField("trigger_at")
    private LocalDateTime triggerAt;

    /** 网络延迟补偿（毫秒），在 trigger_at 前提前发包 */
    @TableField("advance_ms")
    private Integer advanceMs;

    @TableField("execution_mode")
    private ExecutionMode executionMode;

    @TableField("status")
    private TaskStatus status;

    @TableField("remark")
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
