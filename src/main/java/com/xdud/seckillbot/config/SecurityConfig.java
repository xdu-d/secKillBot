package com.xdud.seckillbot.config;

import com.xdud.seckillbot.api.dto.response.ApiResponse;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.security.JwtAuthenticationFilter;
import com.xdud.seckillbot.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          UserDetailsService userDetailsService,
                          ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    res.getWriter().write(objectMapper.writeValueAsString(
                            ApiResponse.fail(ErrorCode.UNAUTHORIZED)));
                })
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/login").permitAll()
                .antMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .headers().frameOptions().sameOrigin()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
