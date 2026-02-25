package com.xdud.seckillbot.security;

import com.xdud.seckillbot.domain.entity.SysUser;
import com.xdud.seckillbot.domain.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .disabled(!Boolean.TRUE.equals(sysUser.getEnabled()))
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .authorities(Collections.emptyList())
                .build();
    }
}
