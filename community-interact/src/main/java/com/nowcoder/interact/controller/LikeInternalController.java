package com.nowcoder.interact.controller;

import com.nowcoder.community.util.Result;
import com.nowcoder.interact.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/like")
public class LikeInternalController {

    @Autowired
    private LikeService likeService;

    @GetMapping("/user/{userId}")
    public Result findUserLikeCount(@PathVariable("userId") int userId) {
        long count = likeService.findUserLikeCount(userId);
        return Result.ok().put("likeCount", count);
    }

}
