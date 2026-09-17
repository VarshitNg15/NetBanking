package com.netbanking.notification;

import com.netbanking.notification.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
