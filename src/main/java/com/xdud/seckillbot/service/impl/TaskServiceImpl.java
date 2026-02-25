package com.xdud.seckillbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xdud.seckillbot.api.dto.request.TaskCreateRequest;
import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.domain.entity.Task;
import com.xdud.seckillbot.domain.entity.TaskAccount;
import com.xdud.seckillbot.domain.enums.ExecutionMode;
import com.xdud.seckillbot.domain.enums.TaskStatus;
import com.xdud.seckillbot.domain.mapper.TaskAccountMapper;
import com.xdud.seckillbot.domain.mapper.TaskMapper;
import com.xdud.seckillbot.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final TaskAccountMapper taskAccountMapper;

    public TaskServiceImpl(TaskMapper taskMapper, TaskAccountMapper taskAccountMapper) {
        this.taskMapper = taskMapper;
        this.taskAccountMapper = taskAccountMapper;
    }

    @Override
    public IPage<Task> listTasks(Page<Task> page) {
        return taskMapper.selectPage(page, null);
    }

    @Override
    public Task getTaskById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(TaskCreateRequest request) {
        Task task = new Task();
        task.setName(request.getName());
        task.setPlatformType(request.getPlatformType());
        task.setProductId(request.getProductId());
        task.setProductName(request.getProductName());
        task.setProductParams(request.getProductParams());
        task.setTriggerAt(request.getTriggerAt());
        task.setAdvanceMs(request.getAdvanceMs() != null ? request.getAdvanceMs() : 0);
        task.setExecutionMode(request.getExecutionMode() != null ? request.getExecutionMode() : ExecutionMode.PARALLEL);
        task.setStatus(TaskStatus.DRAFT);
        task.setRemark(request.getRemark());
        taskMapper.insert(task);

        if (!CollectionUtils.isEmpty(request.getAccountIds())) {
            List<TaskAccount> relations = request.getAccountIds().stream()
                    .map(accountId -> {
                        TaskAccount ta = new TaskAccount();
                        ta.setTaskId(task.getId());
                        ta.setAccountId(accountId);
                        return ta;
                    }).collect(Collectors.toList());
            relations.forEach(taskAccountMapper::insert);
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task updateTask(Long id, TaskCreateRequest request) {
        Task task = getTaskById(id);
        if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.SCHEDULED) {
            throw new BizException(ErrorCode.TASK_STATUS_CONFLICT, "运行中或已调度的任务不允许修改");
        }
        task.setName(request.getName());
        task.setPlatformType(request.getPlatformType());
        task.setProductId(request.getProductId());
        task.setProductName(request.getProductName());
        task.setProductParams(request.getProductParams());
        task.setTriggerAt(request.getTriggerAt());
        if (request.getAdvanceMs() != null) {
            task.setAdvanceMs(request.getAdvanceMs());
        }
        if (request.getExecutionMode() != null) {
            task.setExecutionMode(request.getExecutionMode());
        }
        task.setRemark(request.getRemark());
        taskMapper.updateById(task);

        if (!CollectionUtils.isEmpty(request.getAccountIds())) {
            taskAccountMapper.delete(
                    new LambdaQueryWrapper<TaskAccount>().eq(TaskAccount::getTaskId, id));
            request.getAccountIds().forEach(accountId -> {
                TaskAccount ta = new TaskAccount();
                ta.setTaskId(id);
                ta.setAccountId(accountId);
                taskAccountMapper.insert(ta);
            });
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new BizException(ErrorCode.TASK_STATUS_CONFLICT, "运行中的任务不允许删除");
        }
        taskMapper.deleteById(id);
        taskAccountMapper.delete(
                new LambdaQueryWrapper<TaskAccount>().eq(TaskAccount::getTaskId, id));
    }

    @Override
    public void scheduleTask(Long id) {
        int updated = taskMapper.casUpdateStatus(id, TaskStatus.DRAFT.getValue(), TaskStatus.SCHEDULED.getValue());
        if (updated == 0) {
            throw new BizException(ErrorCode.TASK_STATUS_CONFLICT, "任务当前状态不允许调度（需为 DRAFT）");
        }
    }

    @Override
    public void cancelTask(Long id) {
        int updated = taskMapper.casUpdateStatus(id, TaskStatus.SCHEDULED.getValue(), TaskStatus.CANCELLED.getValue());
        if (updated == 0) {
            throw new BizException(ErrorCode.TASK_STATUS_CONFLICT, "任务当前状态不允许取消（需为 SCHEDULED）");
        }
    }
}
