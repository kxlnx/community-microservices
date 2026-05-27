package com.nowcoder.interact.service;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.SensitiveFilter;
import com.nowcoder.interact.dao.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    public Map<Integer, Integer> findCommentCounts(int entityType, List<Integer> entityIds) {
        Map<Integer, Integer> result = new HashMap<>();
        if (entityIds == null || entityIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = commentMapper.selectCountsByEntities(entityType, entityIds);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object entityId = row.get("entityId");
                Object count = row.get("count");
                if (entityId instanceof Number && count instanceof Number) {
                    result.put(((Number) entityId).intValue(), ((Number) count).intValue());
                }
            }
        }
        return result;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        // 原子更新帖子评论数量（避免SELECT+UPDATE行锁竞争）
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            discussPostService.incrementCommentCount(comment.getEntityId());
        }

        return rows;
    }

    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }

    public List<Comment> findCommentsByUserId(int userId, int offset, int limit) {
        return commentMapper.selectCommentsByUserId(userId, offset, limit);
    }

    public int findCommentCountByUserId(int userId) {
        return commentMapper.selectCountByUserId(userId);
    }

}
