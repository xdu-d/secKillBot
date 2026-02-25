package com.xdud.seckillbot.platform.spi;

import com.xdud.seckillbot.platform.model.AuthContext;

/**
 * 认证提供者：负责登录、刷新、判断是否需要刷新。
 */
public interface AuthProvider {

    /**
     * 用户名密码登录，返回 AuthContext（含 token/cookie 等）。
     *
     * @param credentialJson AES 解密后的凭据 JSON（用户名、密码、手机号等）
     */
    AuthContext login(String credentialJson);

    /**
     * 主动刷新 Token。
     */
    AuthContext refresh(AuthContext current);

    /**
     * 判断当前 AuthContext 是否需要刷新（提前 30 分钟返回 true）。
     */
    boolean needsRefresh(AuthContext context);
}
