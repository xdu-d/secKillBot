package com.xdud.seckillbot.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.response.ApiResponse;
import com.xdud.seckillbot.domain.entity.ExecutionLog;
import com.xdud.seckillbot.domain.enums.ExecutionResult;
import com.xdud.seckillbot.domain.mapper.ExecutionLogMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class ExecutionLogController {

    private final ExecutionLogMapper executionLogMapper;

    public ExecutionLogController(ExecutionLogMapper executionLogMapper) {
        this.executionLogMapper = executionLogMapper;
    }

    @GetMapping
    public ApiResponse<IPage<ExecutionLog>> listLogs(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) ExecutionResult result,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        LambdaQueryWrapper<ExecutionLog> wrapper = new LambdaQueryWrapper<ExecutionLog>()
                .select(ExecutionLog.class, f -> !f.getColumn().equals("response_body"))
                .eq(taskId != null, ExecutionLog::getTaskId, taskId)
                .eq(result != null, ExecutionLog::getResult, result)
                .orderByDesc(ExecutionLog::getActualAt);

        return ApiResponse.ok(executionLogMapper.selectPage(new Page<>(current, size), wrapper));
    }
}
