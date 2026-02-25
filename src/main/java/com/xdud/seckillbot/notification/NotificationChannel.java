package com.xdud.seckillbot.notification;

import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.domain.entity.Task;

/**
 * 通知渠道接口，支持多种实现（Server酱微信推送、邮件等）。
 */
public interface NotificationChannel {

    void sendSuccess(Task task, Account account, String orderId);

    void sendFailure(Task task, Account account, String reason);

    void sendAuthExpired(Account account);
}
