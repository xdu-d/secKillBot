package com.xdud.seckillbot.api.dto.request;

import com.xdud.seckillbot.domain.enums.ExecutionMode;
import com.xdud.seckillbot.domain.enums.PlatformType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateRequest {

    private String name;
    private PlatformType platformType;
    private String productId;
    private String productName;
    private String productParams;
    private LocalDateTime triggerAt;
    private Integer advanceMs;
    private ExecutionMode executionMode;
    private String remark;
    private List<Long> accountIds;
}
