package com.xdud.seckillbot.api.dto.request;

import com.xdud.seckillbot.domain.enums.PlatformType;
import lombok.Data;

@Data
public class AccountCreateRequest {

    private PlatformType platformType;
    private String name;
    private String phone;
    private String credentialJson;
    private String remark;
}
