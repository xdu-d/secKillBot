package com.xdud.seckillbot.api.dto.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String username;
    private String nickname;
    private long expiresIn;

    public LoginResponse(String token, String username, String nickname, long expiresIn) {
        this.token = token;
        this.username = username;
        this.nickname = nickname;
        this.expiresIn = expiresIn;
    }
}
