package com.xdud.seckillbot.common.constant;

public final class Constants {

    private Constants() {}

    public static final String REDIS_TASK_LOCK_PREFIX = "seckill:task:lock:";
    public static final String REDIS_AUTH_CONTEXT_PREFIX = "seckill:auth:";
    public static final int REDIS_AUTH_CONTEXT_TTL_HOURS = 24;

    public static final String JWT_BEARER_PREFIX = "Bearer ";
    public static final String JWT_HEADER = "Authorization";

    public static final int SCHEDULER_WAKEUP_ADVANCE_MS = 500;
    public static final int SCHEDULER_SPIN_THRESHOLD_MS = 50;
    public static final int AUTH_REFRESH_AHEAD_MINUTES = 30;
}
