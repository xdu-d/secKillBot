package com.xdud.seckillbot.api.controller;

import com.xdud.seckillbot.api.dto.response.ApiResponse;
import com.xdud.seckillbot.domain.entity.Platform;
import com.xdud.seckillbot.service.PlatformService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping
    public ApiResponse<List<Platform>> listPlatforms() {
        return ApiResponse.ok(platformService.listEnabledPlatforms());
    }
}
