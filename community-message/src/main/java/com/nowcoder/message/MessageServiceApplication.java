package com.nowcoder.message;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.nowcoder.message", "com.nowcoder.community"})
@EnableFeignClients(basePackages = "com.nowcoder.community.feign")
@MapperScan("com.nowcoder.community.dao")
public class MessageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
