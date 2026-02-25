package com.xdud.seckillbot.platform.spi;

import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;

/**
 * 订单提交器：处理下单前置准备、提交订单、查询订单状态。
 */
public interface OrderSubmitter {

    /**
     * 下单前置准备（如加购物车、锁定座位等平台特定操作）。
     */
    void preOrderSetup(OrderRequest request);

    /**
     * 提交订单。
     */
    OrderResult submitOrder(OrderRequest request);

    /**
     * 查询订单状态。
     */
    String queryOrderStatus(String orderId, AuthContext authContext);
}
