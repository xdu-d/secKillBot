package com.xdud.seckillbot.platform.impl.damai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大麦账号凭据 POJO，对应 account.credential_json 解密后的 JSON 结构。
 * deviceId / umidToken / appKey 需通过抓包 App 请求获取。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamaiCredential {

    private String phone;       // 手机号
    private String deviceId;    // UUID，首次生成后固定（对应 cna cookie）
    private String umidToken;   // 设备指纹 token（App 启动时从 /umid/getUmid 抓包获取）
    private String appKey;      // mtop AppKey（从 App 请求 URL 参数抓包，如 "12574478"）
    // 登录后回写字段（AccountService 自动维护）
    private String h5Tk;        // _m_h5_tk cookie 完整值（"32位token_时间戳"格式）
    private String h5TkEnc;     // _m_h5_tk_enc cookie 值
}
