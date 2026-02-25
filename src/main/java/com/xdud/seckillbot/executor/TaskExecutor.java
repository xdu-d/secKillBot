package com.xdud.seckillbot.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.domain.entity.ExecutionLog;
import com.xdud.seckillbot.domain.entity.Task;
import com.xdud.seckillbot.domain.entity.TaskAccount;
import com.xdud.seckillbot.domain.enums.ExecutionMode;
import com.xdud.seckillbot.domain.enums.ExecutionResult;
import com.xdud.seckillbot.domain.enums.TaskStatus;
import com.xdud.seckillbot.domain.mapper.AccountMapper;
import com.xdud.seckillbot.domain.mapper.ExecutionLogMapper;
import com.xdud.seckillbot.domain.mapper.TaskAccountMapper;
import com.xdud.seckillbot.domain.mapper.TaskMapper;
import com.xdud.seckillbot.notification.NotificationChannel;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import com.xdud.seckillbot.platform.registry.PlatformRegistry;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import com.xdud.seckillbot.security.AesGcmCrypto;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 任务执行引擎：支持 PARALLEL（多账号并发）和 SEQUENTIAL（顺序执行）模式。
 */
@Component
public class TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);

    private final TaskMapper taskMapper;
    private final TaskAccountMapper taskAccountMapper;
    private final AccountMapper accountMapper;
    private final ExecutionLogMapper executionLogMapper;
    private final PlatformRegistry platformRegistry;
    private final AesGcmCrypto aesGcmCrypto;
    private final List<NotificationChannel> notificationChannels;
    private final ThreadPoolExecutor taskExecutorPool;
    private final Gson gson = new Gson();

    public TaskExecutor(TaskMapper taskMapper,
                        TaskAccountMapper taskAccountMapper,
                        AccountMapper accountMapper,
                        ExecutionLogMapper executionLogMapper,
                        PlatformRegistry platformRegistry,
                        AesGcmCrypto aesGcmCrypto,
                        List<NotificationChannel> notificationChannels,
                        @Qualifier("taskExecutorPool") ThreadPoolExecutor taskExecutorPool) {
        this.taskMapper = taskMapper;
        this.taskAccountMapper = taskAccountMapper;
        this.accountMapper = accountMapper;
        this.executionLogMapper = executionLogMapper;
        this.platformRegistry = platformRegistry;
        this.aesGcmCrypto = aesGcmCrypto;
        this.notificationChannels = notificationChannels;
        this.taskExecutorPool = taskExecutorPool;
    }

    public void execute(Task task) {
        int updated = taskMapper.casUpdateStatus(
                task.getId(), TaskStatus.SCHEDULED.getValue(), TaskStatus.RUNNING.getValue());
        if (updated == 0) {
            log.warn("任务 {} 状态 CAS 更新失败，跳过执行", task.getId());
            return;
        }

        List<TaskAccount> taskAccounts = taskAccountMapper.selectList(
                new LambdaQueryWrapper<TaskAccount>().eq(TaskAccount::getTaskId, task.getId())
                        .orderByAsc(TaskAccount::getSortOrder));

        List<Account> accounts = taskAccounts.stream()
                .map(ta -> accountMapper.selectById(ta.getAccountId()))
                .filter(a -> a != null)
                .collect(Collectors.toList());

        PlatformAdapter adapter = platformRegistry.getAdapter(task.getPlatformType());

        if (task.getExecutionMode() == ExecutionMode.PARALLEL) {
            executeParallel(task, accounts, adapter);
        } else {
            executeSequential(task, accounts, adapter);
        }
    }

    private void executeParallel(Task task, List<Account> accounts, PlatformAdapter adapter) {
        List<CompletableFuture<OrderResult>> futures = accounts.stream()
                .map(account -> CompletableFuture.supplyAsync(
                        () -> executeForAccount(task, account, adapter),
                        taskExecutorPool))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        updateTaskFinalStatus(task);
    }

    private void executeSequential(Task task, List<Account> accounts, PlatformAdapter adapter) {
        for (Account account : accounts) {
            OrderResult result = executeForAccount(task, account, adapter);
            if (result.getResult() == ExecutionResult.SUCCESS) {
                break; // 顺序模式：成功后停止
            }
        }
        updateTaskFinalStatus(task);
    }

    private OrderResult executeForAccount(Task task, Account account, PlatformAdapter adapter) {
        long startMs = System.currentTimeMillis();
        LocalDateTime actualAt = LocalDateTime.now();
        OrderResult result;

        try {
            AuthContext authContext = resolveAuthContext(account, adapter);
            OrderRequest request = buildOrderRequest(task, authContext);
            adapter.preOrderSetup(request);
            result = adapter.submitOrder(request);
        } catch (Exception e) {
            log.error("账号 {} 下单异常", account.getId(), e);
            result = OrderResult.fail(ExecutionResult.FAILED, e.getMessage(), null);
        }

        long durationMs = System.currentTimeMillis() - startMs;
        saveExecutionLog(task, account, result, actualAt, durationMs);

        if (result.getResult() == ExecutionResult.SUCCESS) {
            notificationChannels.forEach(ch -> ch.sendSuccess(task, account, result.getOrderId()));
        }

        return result;
    }

    private AuthContext resolveAuthContext(Account account, PlatformAdapter adapter) {
        if (account.getAuthContext() != null) {
            AuthContext ctx = gson.fromJson(aesGcmCrypto.decrypt(account.getAuthContext()), AuthContext.class);
            if (!adapter.needsRefresh(ctx)) {
                return ctx;
            }
        }
        String credentialJson = aesGcmCrypto.decrypt(account.getCredentialJson());
        return adapter.login(credentialJson);
    }

    private OrderRequest buildOrderRequest(Task task, AuthContext authContext) {
        OrderRequest request = new OrderRequest();
        request.setProductId(task.getProductId());
        request.setProductName(task.getProductName());
        request.setQuantity(1);
        request.setAuthContext(authContext);
        if (task.getProductParams() != null) {
            request.setExtras(gson.fromJson(task.getProductParams(),
                    new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>() {}.getType()));
        }
        return request;
    }

    private void saveExecutionLog(Task task, Account account, OrderResult result,
                                   LocalDateTime actualAt, long durationMs) {
        ExecutionLog log = new ExecutionLog();
        log.setTaskId(task.getId());
        log.setAccountId(account.getId());
        log.setResult(result.getResult());
        log.setActualAt(actualAt);
        log.setDurationMs(durationMs);
        log.setOrderId(result.getOrderId());
        log.setResponseBody(result.getRawResponse());
        log.setErrorMsg(result.getMessage());
        executionLogMapper.insert(log);
    }

    private void updateTaskFinalStatus(Task task) {
        List<ExecutionLog> logs = executionLogMapper.selectList(
                new LambdaQueryWrapper<ExecutionLog>().eq(ExecutionLog::getTaskId, task.getId()));
        boolean anySuccess = logs.stream().anyMatch(l -> l.getResult() == ExecutionResult.SUCCESS);
        String finalStatus = anySuccess ? TaskStatus.SUCCESS.getValue() : TaskStatus.FAILED.getValue();
        taskMapper.casUpdateStatus(task.getId(), TaskStatus.RUNNING.getValue(), finalStatus);
    }
}
