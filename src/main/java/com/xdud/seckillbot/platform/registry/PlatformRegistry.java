package com.xdud.seckillbot.platform.registry;

import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 平台适配器注册表：Spring 容器启动时自动收集所有 PlatformAdapter 实现，
 * 按 PlatformType 索引，供调度/执行引擎使用。
 */
@Component
public class PlatformRegistry {

    private final Map<PlatformType, PlatformAdapter> adapters = new EnumMap<>(PlatformType.class);

    public PlatformRegistry(List<PlatformAdapter> adapterList) {
        for (PlatformAdapter adapter : adapterList) {
            adapters.put(adapter.getPlatformType(), adapter);
        }
    }

    public PlatformAdapter getAdapter(PlatformType type) {
        PlatformAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new BizException(ErrorCode.PLATFORM_NOT_SUPPORTED,
                    "平台适配器未找到: " + type.getDescription());
        }
        return adapter;
    }

    public boolean isSupported(PlatformType type) {
        return adapters.containsKey(type);
    }
}
