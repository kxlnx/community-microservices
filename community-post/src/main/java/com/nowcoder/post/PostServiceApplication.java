package com.nowcoder.post;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.nowcoder.post", "com.nowcoder.community"})
@EnableFeignClients(basePackages = "com.nowcoder.community.feign")
public class PostServiceApplication {
    public static void main(String[] args) { SpringApplication.run(PostServiceApplication.class, args); }
}
