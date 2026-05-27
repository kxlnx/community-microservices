package com.nowcoder.interact.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.interact.service.CommentService;
import com.nowcoder.interact.service.DiscussPostService;
import com.nowcoder.interact.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/like")
    public String like(@RequestParam("entityType") int entityType,
                       @RequestParam("entityId") int entityId,
                       @RequestParam("postId") int postId,
                       @RequestHeader("X-User-Id") int userId) {

        // 服务端解析 entityUserId，不信任客户端
        int entityUserId = resolveEntityUserId(entityType, entityId, postId);

        // 点赞
        likeService.like(userId, entityType, entityId, entityUserId);

        // 数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 状态
        int likeStatus = likeService.findEntityLikeStatus(userId, entityType, entityId);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 触发点赞事件
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(userId)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST) {
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return CommunityUtil.getJSONString(0, null, map);
    }

    // 服务端解析实体所属用户，防止客户端伪造
    private int resolveEntityUserId(int entityType, int entityId, int postId) {
        if (entityType == ENTITY_TYPE_POST) {
            DiscussPost post = discussPostService.findDiscussPostById(entityId);
            return post != null ? post.getUserId() : 0;
        } else if (entityType == ENTITY_TYPE_COMMENT) {
            Comment comment = commentService.findCommentById(entityId);
            return comment != null ? comment.getUserId() : 0;
        }
        return 0;
    }

}
