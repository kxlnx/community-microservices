package com.nowcoder.community.feign;

import com.nowcoder.community.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "community-search", url = "${feign.client.search.url:http://localhost:8085}")
public interface SearchClient {

    @GetMapping("/search")
    Result search(@RequestParam("keyword") String keyword,
                  @RequestParam("current") int current,
                  @RequestParam("limit") int limit);

}
