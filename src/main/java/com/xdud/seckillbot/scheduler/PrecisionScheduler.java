package com.xdud.seckillbot.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdud.seckillbot.common.constant.Constants;
import com.xdud.seckillbot.domain.entity.Task;
import com.xdud.seckillbot.domain.enums.TaskStatus;
import com.xdud.seckillbot.domain.mapper.TaskMapper;
import com.xdud.seckillbot.executor.TaskExecutor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 毫秒级精准调度器。
 *
 * <p>调度策略：
 * <ol>
 *   <li>每秒扫描未来 5 秒内的 SCHEDULED 任务</li>
 *   <li>提前 500ms 唤醒执行线程</li>
 *   <li>最后 50ms 进入 CPU 自旋等待，精度 &lt;1ms</li>
 *   <li>Redisson 分布式锁防止多实例重复执行</li>
 * </ol>
 */
@Component
public class PrecisionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrecisionScheduler.class);

    private final TaskMapper taskMapper;
    private final TaskExecutor taskExecutor;
    private final RedissonClient redissonClient;
    private final ThreadPoolExecutor schedulerPool;

    public PrecisionScheduler(TaskMapper taskMapper,
                               TaskExecutor taskExecutor,
                               RedissonClient redissonClient,
                               @Qualifier("schedulerPool") ThreadPoolExecutor schedulerPool) {
        this.taskMapper = taskMapper;
        this.taskExecutor = taskExecutor;
        this.redissonClient = redissonClient;
        this.schedulerPool = schedulerPool;
    }

    @Scheduled(fixedDelay = 1000)
    public void scanAndSchedule() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scanWindow = now.plusSeconds(5);

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, TaskStatus.SCHEDULED)
                        .between(Task::getTriggerAt, now, scanWindow));

        for (Task task : tasks) {
            schedulerPool.submit(() -> handleTask(task));
        }
    }

    private void handleTask(Task task) {
        String lockKey = Constants.REDIS_TASK_LOCK_PREFIX + task.getId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(0, 35, TimeUnit.SECONDS)) {
                log.debug("任务 {} 已被其他实例锁定，跳过", task.getId());
                return;
            }
            precisionWait(task);
            taskExecutor.execute(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("任务 {} 调度被中断", task.getId());
        } catch (Exception e) {
            log.error("任务 {} 执行异常", task.getId(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 精准等待到 triggerAt - advanceMs 时刻。
     * 最后 50ms 使用 CPU 自旋确保 <1ms 精度。
     */
    private void precisionWait(Task task) throws InterruptedException {
        long targetEpochMs = toEpochMs(task.getTriggerAt())
                - (task.getAdvanceMs() != null ? task.getAdvanceMs() : 0);

        long sleepMs = targetEpochMs - System.currentTimeMillis() - Constants.SCHEDULER_WAKEUP_ADVANCE_MS;
        if (sleepMs > 0) {
            Thread.sleep(sleepMs);
        }

        // 最后 50ms 自旋
        long spinTarget = targetEpochMs - Constants.SCHEDULER_SPIN_THRESHOLD_MS;
        while (System.currentTimeMillis() < spinTarget) {
            Thread.sleep(0, 500_000); // ~0.5ms
        }
        // 最后几毫秒纯自旋
        while (System.currentTimeMillis() < targetEpochMs) {
            // busy wait
        }
    }

    private long toEpochMs(LocalDateTime ldt) {
        return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
