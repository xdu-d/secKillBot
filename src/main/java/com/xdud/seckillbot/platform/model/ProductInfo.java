package com.xdud.seckillbot.platform.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductInfo {

    private String productId;
    private String productName;
    private BigDecimal price;
    private boolean inStock;
    private int stockCount;
    private String rawJson;
}
