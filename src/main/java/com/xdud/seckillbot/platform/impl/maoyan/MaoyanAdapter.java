package com.xdud.seckillbot.platform.impl.maoyan;

import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import com.xdud.seckillbot.platform.model.ProductInfo;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * 猫眼平台适配器（Phase 3 完整实现）。
 * 关键点：场次选择、座位锁定、快速下单流程。
 */
@Component
public class MaoyanAdapter implements PlatformAdapter {

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.MAOYAN;
    }

    @Override
    public AuthContext login(String credentialJson) {
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
    }

    @Override
    public AuthContext refresh(AuthContext current) {
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
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
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
    }

    @Override
    public boolean hasStock(String productId, AuthContext authContext) {
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
    }

    @Override
    public void preOrderSetup(OrderRequest request) {
        // TODO: Phase 3 - 猫眼场次锁定逻辑
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
    }

    @Override
    public OrderResult submitOrder(OrderRequest request) {
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
    }

    @Override
    public String queryOrderStatus(String orderId, AuthContext authContext) {
        throw new UnsupportedOperationException("猫眼适配器将在 Phase 3 实现");
    }
}
