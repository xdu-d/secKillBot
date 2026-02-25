package com.xdud.seckillbot.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xdud.seckillbot.domain.enums.AccountStatus;
import com.xdud.seckillbot.domain.enums.PlatformType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account")
public class Account {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("platform_type")
    private PlatformType platformType;

    @TableField("name")
    private String name;

    @TableField("phone")
    private String phone;

    /** AES-GCM 加密存储的登录凭据（用户名/密码/token 等） */
    @TableField("credential_json")
    private String credentialJson;

    /** AES-GCM 加密存储的运行时 AuthContext（token/cookie/设备ID） */
    @TableField("auth_context")
    private String authContext;

    @TableField("status")
    private AccountStatus status;

    @TableField("auth_expires_at")
    private LocalDateTime authExpiresAt;

    @TableField("remark")
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
