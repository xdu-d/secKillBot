package com.xdud.seckillbot.platform.impl.maoyan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 猫眼账号凭据 POJO，对应 account.credential_json 解密后的 JSON 结构。
 * appKey / appSecret 需通过抓包或逆向 App 获取。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaoyanCredential {

    private String phone;       // 手机号
    private String deviceId;    // UUID
    private String appVersion;  // App 版本（抓包 User-Agent 获取，如 "9.2.0"）
    private String appKey;      // 请求头 x-app-key（抓包获取）
    private String appSecret;   // 签名密钥（逆向 App 获取）
    // 登录后回写字段
    private String token;       // access token
    private String userId;      // 猫眼用户 ID
}
