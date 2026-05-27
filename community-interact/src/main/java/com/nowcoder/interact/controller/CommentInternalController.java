package com.nowcoder.interact.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.Result;
import com.nowcoder.interact.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class CommentInternalController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/comments/by-entity")
    public List<Comment> findCommentsByEntity(@RequestParam("entityType") int entityType,
                                               @RequestParam("entityId") int entityId,
                                               @RequestParam("offset") int offset,
                                               @RequestParam("limit") int limit) {
        return commentService.findCommentsByEntity(entityType, entityId, offset, limit);
    }

    @PostMapping("/comments/counts")
    public Map<Integer, Integer> findCommentCounts(@RequestParam("entityType") int entityType,
                                                    @RequestBody List<Integer> entityIds) {
        return commentService.findCommentCounts(entityType, entityIds);
    }

    @GetMapping("/comments/user/{userId}")
    public Result findByUserId(@PathVariable("userId") int userId,
                                @RequestParam("offset") int offset,
                                @RequestParam("limit") int limit) {
        List<Comment> comments = commentService.findCommentsByUserId(userId, offset, limit);
        return Result.ok().put("comments", comments);
    }

    @GetMapping("/comments/user/{userId}/count")
    public Result countByUserId(@PathVariable("userId") int userId) {
        int count = commentService.findCommentCountByUserId(userId);
        return Result.ok().put("count", count);
    }

}
