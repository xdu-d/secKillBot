package com.xdud.seckillbot.service;

import com.xdud.seckillbot.domain.entity.Platform;

import java.util.List;

public interface PlatformService {

    List<Platform> listEnabledPlatforms();
}
