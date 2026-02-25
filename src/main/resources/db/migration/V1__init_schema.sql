-- =====================================================
-- secKillBot 数据库初始化脚本
-- 支持 MySQL 8 (生产) 和 H2 (开发，MODE=MySQL)
-- =====================================================

-- 平台配置表
CREATE TABLE IF NOT EXISTS platform
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_type VARCHAR(32)  NOT NULL UNIQUE COMMENT '平台标识（imoutai/damai/maoyan）',
    name          VARCHAR(64)  NOT NULL COMMENT '平台名称',
    base_url      VARCHAR(256) NOT NULL COMMENT '平台 API base URL',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    config_json   TEXT COMMENT '平台自定义配置（JSON）',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

-- 预置三个平台
INSERT INTO platform (platform_type, name, base_url, enabled)
VALUES ('imoutai', 'i茅台', 'https://m.moutai519.com.cn', 1),
       ('damai', '大麦网', 'https://mtop.damai.cn', 1),
       ('maoyan', '猫眼', 'https://api.maoyan.com', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 账号表
CREATE TABLE IF NOT EXISTS account
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_type   VARCHAR(32)  NOT NULL COMMENT '平台标识',
    name            VARCHAR(64)  NOT NULL COMMENT '账号备注名',
    phone           VARCHAR(20) COMMENT '手机号（脱敏显示用）',
    credential_json TEXT COMMENT 'AES-GCM 加密的登录凭据 JSON',
    auth_context    TEXT COMMENT 'AES-GCM 加密的运行时 AuthContext',
    status          VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT 'active/expired/banned/disabled',
    auth_expires_at DATETIME(3) COMMENT 'Token 过期时间',
    remark          VARCHAR(256) COMMENT '备注',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_platform_type (platform_type),
    INDEX idx_status (status)
);

-- 秒杀任务表
CREATE TABLE IF NOT EXISTS task
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(128) NOT NULL COMMENT '任务名称',
    platform_type  VARCHAR(32)  NOT NULL COMMENT '平台标识',
    product_id     VARCHAR(128) NOT NULL COMMENT '商品 ID',
    product_name   VARCHAR(256) COMMENT '商品名称（冗余，便于展示）',
    product_params TEXT COMMENT '商品附加参数（场次/SKU等，JSON）',
    trigger_at     DATETIME(3)  NOT NULL COMMENT '精确触发时间（毫秒精度）',
    advance_ms     INT          NOT NULL DEFAULT 0 COMMENT '网络延迟补偿毫秒数',
    execution_mode VARCHAR(16)  NOT NULL DEFAULT 'parallel' COMMENT 'parallel/sequential',
    status         VARCHAR(16)  NOT NULL DEFAULT 'draft' COMMENT 'draft/scheduled/running/success/failed/cancelled',
    remark         VARCHAR(256) COMMENT '备注',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_status_trigger (status, trigger_at)
);

-- 任务-账号多对多关联表
CREATE TABLE IF NOT EXISTS task_account
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT NOT NULL COMMENT '任务 ID',
    account_id BIGINT NOT NULL COMMENT '账号 ID',
    sort_order INT    NOT NULL DEFAULT 0 COMMENT '顺序执行时的优先级',
    UNIQUE KEY uk_task_account (task_id, account_id),
    INDEX idx_ta_task_id (task_id),
    INDEX idx_ta_account_id (account_id)
);

-- 执行日志表
CREATE TABLE IF NOT EXISTS execution_log
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT      NOT NULL COMMENT '任务 ID',
    account_id    BIGINT      NOT NULL COMMENT '账号 ID',
    result        VARCHAR(16) NOT NULL COMMENT 'success/failed/no_stock/auth_expired/network_error/timeout/skipped',
    actual_at     DATETIME(3) NOT NULL COMMENT '实际执行时间（毫秒精度）',
    duration_ms   BIGINT COMMENT '耗时（毫秒）',
    response_body TEXT COMMENT '平台返回的原始响应（调试用）',
    order_id      VARCHAR(128) COMMENT '平台订单号（成功时有值）',
    error_msg     VARCHAR(512) COMMENT '错误信息',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_el_task_id (task_id),
    INDEX idx_el_account_id (account_id),
    INDEX idx_el_result (result)
);

-- 管理用户表
CREATE TABLE IF NOT EXISTS sys_user
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录用户名',
    password   VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希密码',
    nickname   VARCHAR(64) COMMENT '显示名称',
    enabled    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

-- 默认管理员账号（密码：admin123，BCrypt 加密）
-- 生产环境请在首次登录后立即修改密码
INSERT INTO sys_user (username, password, nickname, enabled)
VALUES ('admin', '$2b$10$iaUiwn.YtKugPcUSAy6z6OkFTcFXnCzAfVrXiMk8L4x5SFziooKy2', '管理员', 1)
ON DUPLICATE KEY UPDATE username = username;
