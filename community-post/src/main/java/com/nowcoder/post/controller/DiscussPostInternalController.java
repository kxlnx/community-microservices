package com.nowcoder.post.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.Result;
import com.nowcoder.post.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/discuss")
public class DiscussPostInternalController {

    @Autowired
    private DiscussPostService discussPostService;

    @GetMapping("/{id}")
    public Result findPostById(@PathVariable("id") int id) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        if (post == null) {
            return Result.fail(404, "帖子不存在");
        }
        return Result.ok().put("post", post);
    }

    @PutMapping("/{id}/increment-comment")
    public Result incrementCommentCount(@PathVariable("id") int id) {
        discussPostService.incrementCommentCount(id);
        return Result.ok();
    }

    @PostMapping("/batch")
    public Result findPostsByIds(@RequestBody List<Integer> ids) {
        List<DiscussPost> posts = discussPostService.findDiscussPostsByIds(ids);
        return Result.ok().put("posts", posts);
    }

    @PutMapping("/{id}/update-score")
    public Result updateScore(@PathVariable("id") int id, @RequestParam("score") double score) {
        discussPostService.updateScore(id, score);
        return Result.ok();
    }

    @GetMapping("/user/{userId}")
    public Result findByUserId(@PathVariable("userId") int userId,
                               @RequestParam(defaultValue = "0") int offset,
                               @RequestParam(defaultValue = "10") int limit) {
        List<DiscussPost> posts = discussPostService.findDiscussPosts(userId, offset, limit, 0);
        return Result.ok().put("posts", posts);
    }

    @GetMapping("/user/{userId}/count")
    public Result countByUserId(@PathVariable("userId") int userId) {
        int count = discussPostService.findDiscussPostRows(userId);
        return Result.ok().put("count", count);
    }

}
