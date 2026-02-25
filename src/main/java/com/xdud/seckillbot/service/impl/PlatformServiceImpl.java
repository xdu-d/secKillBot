package com.xdud.seckillbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdud.seckillbot.domain.entity.Platform;
import com.xdud.seckillbot.domain.mapper.PlatformMapper;
import com.xdud.seckillbot.service.PlatformService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformServiceImpl implements PlatformService {

    private final PlatformMapper platformMapper;

    public PlatformServiceImpl(PlatformMapper platformMapper) {
        this.platformMapper = platformMapper;
    }

    @Override
    public List<Platform> listEnabledPlatforms() {
        return platformMapper.selectList(
                new LambdaQueryWrapper<Platform>().eq(Platform::getEnabled, true));
    }
}
