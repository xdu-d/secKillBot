package com.xdud.seckillbot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.request.TaskCreateRequest;
import com.xdud.seckillbot.domain.entity.Task;

public interface TaskService {

    IPage<Task> listTasks(Page<Task> page);

    Task getTaskById(Long id);

    Task createTask(TaskCreateRequest request);

    Task updateTask(Long id, TaskCreateRequest request);

    void deleteTask(Long id);

    void scheduleTask(Long id);

    void cancelTask(Long id);
}
