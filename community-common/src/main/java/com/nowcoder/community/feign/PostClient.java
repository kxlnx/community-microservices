package com.nowcoder.community.feign;

import com.nowcoder.community.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "community-post", url = "${feign.client.post.url:http://localhost:8087}")
public interface PostClient {

    @GetMapping("/internal/discuss/{id}")
    Result findPostById(@PathVariable("id") int id);

    @PutMapping("/internal/discuss/{id}/increment-comment")
    Result incrementCommentCount(@PathVariable("id") int id);

    @PostMapping("/internal/discuss/batch")
    Result findPostsByIds(@RequestBody List<Integer> ids);

    @PutMapping("/internal/discuss/{id}/update-score")
    Result updateScore(@PathVariable("id") int id, @RequestParam("score") double score);

    @GetMapping("/internal/discuss/user/{userId}")
    Result findByUserId(@PathVariable("userId") int userId,
                        @RequestParam("offset") int offset,
                        @RequestParam("limit") int limit);

    @GetMapping("/internal/discuss/user/{userId}/count")
    Result countByUserId(@PathVariable("userId") int userId);

}
