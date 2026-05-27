package com.nowcoder.community.feign;

import com.nowcoder.community.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "community-user", url = "${feign.client.user.url:http://localhost:8086}")
public interface UserClient {

    @GetMapping("/internal/user/{id}")
    User findUserById(@PathVariable("id") int id);

    @GetMapping("/internal/user/name/{name}")
    User findUserByName(@PathVariable("name") String name);

    @PostMapping("/internal/user/batch")
    Map<Integer, User> findUsersByIds(@RequestBody List<Integer> ids);

}
