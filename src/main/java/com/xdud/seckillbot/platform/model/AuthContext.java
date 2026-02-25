package com.xdud.seckillbot.platform.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 平台运行时认证上下文，包含 Token/Cookie/设备 ID 等敏感信息。
 * 序列化后 AES-GCM 加密双写 Redis + DB。
 */
@Data
public class AuthContext {

    /** 平台 accessToken / JWT */
    private String accessToken;

    /** 刷新 Token（部分平台有） */
    private String refreshToken;

    /** Cookie 字符串（大麦/猫眼 Web 端） */
    private String cookieString;

    /** 设备 ID（i茅台必须） */
    private String deviceId;

    /** 平台用户 ID */
    private String platformUserId;

    /** Token 过期时间 */
    private LocalDateTime expiresAt;

    /** 平台自定义扩展字段 */
    private Map<String, String> extras;
}
