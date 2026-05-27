package com.nowcoder.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication(scanBasePackages = {"com.nowcoder.search", "com.nowcoder.community"})
@EnableElasticsearchRepositories(basePackages = "com.nowcoder.community.dao.elasticsearch")
@EnableFeignClients(basePackages = "com.nowcoder.community.feign")
public class SearchServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
