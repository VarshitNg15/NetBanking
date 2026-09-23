package com.netbanking.transaction;

import com.netbanking.transaction.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableScheduling
public class TransactionServiceApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
