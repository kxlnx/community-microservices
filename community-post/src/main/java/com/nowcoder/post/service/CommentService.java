package com.nowcoder.post.service;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.feign.CommentClient;
import com.nowcoder.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentClient commentClient;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentClient.findCommentsByEntity(entityType, entityId, offset, limit);
    }

    public Map<Integer, Integer> findCommentCounts(int entityType, List<Integer> entityIds) {
        return commentClient.findCommentCounts(entityType, entityIds);
    }

}
