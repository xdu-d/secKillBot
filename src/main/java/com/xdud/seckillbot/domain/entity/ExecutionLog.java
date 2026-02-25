package com.xdud.seckillbot.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xdud.seckillbot.domain.enums.ExecutionResult;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("execution_log")
public class ExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("account_id")
    private Long accountId;

    @TableField("result")
    private ExecutionResult result;

    /** 实际执行时间（毫秒精度） */
    @TableField("actual_at")
    private LocalDateTime actualAt;

    /** 耗时（毫秒） */
    @TableField("duration_ms")
    private Long durationMs;

    /** 平台返回的原始响应（用于调试） */
    @TableField("response_body")
    private String responseBody;

    /** 平台返回的订单号 */
    @TableField("order_id")
    private String orderId;

    /** 错误信息 */
    @TableField("error_msg")
    private String errorMsg;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
