package com.xdud.seckillbot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.xdud.seckillbot.domain.mapper")
public class SecKillBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecKillBotApplication.class, args);
    }
}
