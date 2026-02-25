package com.xdud.seckillbot.notification.impl;

import com.xdud.seckillbot.domain.entity.Account;
import com.xdud.seckillbot.domain.entity.Task;
import com.xdud.seckillbot.notification.NotificationChannel;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Server酱微信通知实现（推送到微信服务号）。
 * 需配置 app.notification.serverchan.send-key
 */
@Component
@ConditionalOnProperty(name = "app.notification.serverchan.enabled", havingValue = "true")
public class ServerChanNotification implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(ServerChanNotification.class);
    private static final String SEND_URL = "https://sctapi.ftqq.com/%s.send";

    @Value("${app.notification.serverchan.send-key:}")
    private String sendKey;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void sendSuccess(Task task, Account account, String orderId) {
        String title = "秒杀成功！" + task.getProductName();
        String content = String.format("任务：%s\n账号：%s\n订单号：%s",
                task.getName(), account.getName(), orderId);
        send(title, content);
    }

    @Override
    public void sendFailure(Task task, Account account, String reason) {
        String title = "秒杀失败 - " + task.getProductName();
        String content = String.format("任务：%s\n账号：%s\n原因：%s",
                task.getName(), account.getName(), reason);
        send(title, content);
    }

    @Override
    public void sendAuthExpired(Account account) {
        String title = "账号认证过期提醒";
        String content = String.format("账号 [%s] 的认证已过期，请及时更新 Token。", account.getName());
        send(title, content);
    }

    private void send(String title, String content) {
        if (sendKey == null || sendKey.isEmpty()) {
            log.warn("Server酱 sendKey 未配置，跳过通知");
            return;
        }
        Request request = new Request.Builder()
                .url(String.format(SEND_URL, sendKey))
                .post(new FormBody.Builder()
                        .add("title", title)
                        .add("desp", content)
                        .build())
                .build();
        try {
            httpClient.newCall(request).execute().close();
        } catch (IOException e) {
            log.error("Server酱推送失败", e);
        }
    }
}
