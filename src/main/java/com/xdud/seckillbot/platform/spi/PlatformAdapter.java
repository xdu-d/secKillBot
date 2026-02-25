package com.xdud.seckillbot.platform.spi;

import com.xdud.seckillbot.domain.enums.PlatformType;

/**
 * 平台适配器主接口，每个平台实现此接口并注册为 Spring Bean。
 * PlatformRegistry 会按 PlatformType 索引所有实现。
 */
public interface PlatformAdapter extends AuthProvider, StockChecker, OrderSubmitter {

    /**
     * 返回本适配器支持的平台类型。
     */
    PlatformType getPlatformType();
}
