package com.xdud.seckillbot.api.controller;

import com.xdud.seckillbot.api.dto.request.LoginRequest;
import com.xdud.seckillbot.api.dto.response.ApiResponse;
import com.xdud.seckillbot.api.dto.response.LoginResponse;
import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.domain.entity.SysUser;
import com.xdud.seckillbot.domain.mapper.SysUserMapper;
import com.xdud.seckillbot.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          SysUserMapper sysUserMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserMapper = sysUserMapper;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BizException(ErrorCode.PASSWORD_WRONG);
        }

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));

        String token = jwtTokenProvider.generateToken(request.getUsername());
        LoginResponse resp = new LoginResponse(
                token,
                user.getUsername(),
                user.getNickname(),
                jwtTokenProvider.getExpirationMs() / 1000
        );
        return ApiResponse.ok(resp);
    }
}
