package com.xdud.seckillbot.notification.impl;

import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.domain.entity.Task;
import com.xdud.seckillbot.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认通知实现：仅打日志，始终注册。
 */
@Component
public class LogNotification implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LogNotification.class);

    @Override
    public void sendSuccess(Task task, Account account, String orderId) {
        log.info("[通知] 秒杀成功 - 任务={}, 账号={}, 订单号={}", task.getName(), account.getName(), orderId);
    }

    @Override
    public void sendFailure(Task task, Account account, String reason) {
        log.warn("[通知] 秒杀失败 - 任务={}, 账号={}, 原因={}", task.getName(), account.getName(), reason);
    }

    @Override
    public void sendAuthExpired(Account account) {
        log.warn("[通知] 账号认证过期 - 账号={}", account.getName());
    }
}
