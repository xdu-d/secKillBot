package com.xdud.seckillbot.platform.spi;

import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.ProductInfo;

/**
 * 库存检查器：查询商品信息和库存状态。
 */
public interface StockChecker {

    ProductInfo queryProduct(String productId, AuthContext authContext);

    boolean hasStock(String productId, AuthContext authContext);
}
