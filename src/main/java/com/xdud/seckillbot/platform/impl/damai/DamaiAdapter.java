package com.xdud.seckillbot.platform.impl.damai;

import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import com.xdud.seckillbot.platform.model.ProductInfo;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * 大麦网平台适配器（Phase 3 完整实现）。
 * 关键点：加购物车 → 确认订单 → 支付流程，MD5 签名。
 */
@Component
public class DamaiAdapter implements PlatformAdapter {

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.DAMAI;
    }

    @Override
    public AuthContext login(String credentialJson) {
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }

    @Override
    public AuthContext refresh(AuthContext current) {
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }

    @Override
    public boolean needsRefresh(AuthContext context) {
        if (context == null || context.getExpiresAt() == null) {
            return true;
        }
        return context.getExpiresAt().minusMinutes(30).isBefore(java.time.LocalDateTime.now());
    }

    @Override
    public ProductInfo queryProduct(String productId, AuthContext authContext) {
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }

    @Override
    public boolean hasStock(String productId, AuthContext authContext) {
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }

    @Override
    public void preOrderSetup(OrderRequest request) {
        // TODO: Phase 3 - 大麦加购物车逻辑
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }

    @Override
    public OrderResult submitOrder(OrderRequest request) {
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }

    @Override
    public String queryOrderStatus(String orderId, AuthContext authContext) {
        throw new UnsupportedOperationException("大麦网适配器将在 Phase 3 实现");
    }
}
