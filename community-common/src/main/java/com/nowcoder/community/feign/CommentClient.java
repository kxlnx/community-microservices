package com.nowcoder.community.feign;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "community-interact", url = "${feign.client.interact.url:http://localhost:8083}")
public interface CommentClient {

    @GetMapping("/internal/comments/by-entity")
    List<Comment> findCommentsByEntity(@RequestParam("entityType") int entityType,
                                        @RequestParam("entityId") int entityId,
                                        @RequestParam("offset") int offset,
                                        @RequestParam("limit") int limit);

    @PostMapping("/internal/comments/counts")
    Map<Integer, Integer> findCommentCounts(@RequestParam("entityType") int entityType,
                                             @RequestBody List<Integer> entityIds);

    @GetMapping("/internal/comments/user/{userId}")
    Result findByUserId(@PathVariable("userId") int userId,
                        @RequestParam("offset") int offset,
                        @RequestParam("limit") int limit);

    @GetMapping("/internal/comments/user/{userId}/count")
    Result countByUserId(@PathVariable("userId") int userId);

    @GetMapping("/internal/like/user/{userId}")
    Result findUserLikeCount(@PathVariable("userId") int userId);

}
