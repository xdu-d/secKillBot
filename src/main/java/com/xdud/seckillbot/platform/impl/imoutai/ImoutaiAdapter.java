package com.xdud.seckillbot.platform.impl.imoutai;

import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import com.xdud.seckillbot.platform.model.ProductInfo;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * i茅台平台适配器（Phase 2 完整实现）。
 * 关键点：HMAC-SHA256 请求签名、设备 ID 绑定、预约申购流程。
 */
@Component
public class ImoutaiAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(ImoutaiAdapter.class);

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.IMOUTAI;
    }

    @Override
    public AuthContext login(String credentialJson) {
        // TODO: Phase 2 实现 - 调用 i茅台 App 登录接口，绑定设备 ID，返回 token
        throw new UnsupportedOperationException("i茅台适配器将在 Phase 2 实现");
    }

    @Override
    public AuthContext refresh(AuthContext current) {
        throw new UnsupportedOperationException("i茅台适配器将在 Phase 2 实现");
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
        throw new UnsupportedOperationException("i茅台适配器将在 Phase 2 实现");
    }

    @Override
    public boolean hasStock(String productId, AuthContext authContext) {
        throw new UnsupportedOperationException("i茅台适配器将在 Phase 2 实现");
    }

    @Override
    public void preOrderSetup(OrderRequest request) {
        // i茅台申购无需加购步骤
    }

    @Override
    public OrderResult submitOrder(OrderRequest request) {
        throw new UnsupportedOperationException("i茅台适配器将在 Phase 2 实现");
    }

    @Override
    public String queryOrderStatus(String orderId, AuthContext authContext) {
        throw new UnsupportedOperationException("i茅台适配器将在 Phase 2 实现");
    }
}
