package com.xdud.seckillbot.platform.model;

import lombok.Data;

import java.util.Map;

@Data
public class OrderRequest {

    private String productId;
    private String productName;
    private int quantity;
    /** 平台特定参数（场次ID、SKU、地区等） */
    private Map<String, String> extras;
    private AuthContext authContext;
}
