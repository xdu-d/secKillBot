package com.xdud.seckillbot.platform.impl.imoutai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * i茅台账号凭据 POJO，对应 account.credential_json 解密后的 JSON 结构。
 * 示例：{"phone":"138...","userId":"xxx","token":"xxx","deviceId":"uuid"}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImoutaiCredential {

    private String phone;
    private String userId;
    private String token;
    private String deviceId;
}
