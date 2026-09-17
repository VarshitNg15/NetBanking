package com.netbanking.auth;

import com.netbanking.auth.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
