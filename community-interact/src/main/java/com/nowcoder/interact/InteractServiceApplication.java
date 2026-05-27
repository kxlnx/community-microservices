package com.nowcoder.interact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.nowcoder.interact", "com.nowcoder.community"})
@EnableFeignClients(basePackages = "com.nowcoder.community.feign")
public class InteractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InteractServiceApplication.class, args);
    }
}
