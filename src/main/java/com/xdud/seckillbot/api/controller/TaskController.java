package com.xdud.seckillbot.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.request.TaskCreateRequest;
import com.xdud.seckillbot.api.dto.response.ApiResponse;
import com.xdud.seckillbot.domain.entity.Task;
import com.xdud.seckillbot.service.TaskService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<IPage<Task>> listTasks(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(taskService.listTasks(new Page<>(current, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Task> getTask(@PathVariable Long id) {
        return ApiResponse.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ApiResponse<Task> createTask(@RequestBody TaskCreateRequest request) {
        return ApiResponse.ok(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Task> updateTask(@PathVariable Long id, @RequestBody TaskCreateRequest request) {
        return ApiResponse.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/schedule")
    public ApiResponse<Void> scheduleTask(@PathVariable Long id) {
        taskService.scheduleTask(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelTask(@PathVariable Long id) {
        taskService.cancelTask(id);
        return ApiResponse.ok();
    }
}
