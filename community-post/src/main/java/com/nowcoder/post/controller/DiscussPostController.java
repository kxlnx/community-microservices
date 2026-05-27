package com.nowcoder.post.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.post.service.CommentService;
import com.nowcoder.post.service.LikeService;
import com.nowcoder.post.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.post.service.DiscussPostService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@Validated
public class DiscussPostController implements CommunityConstant {

    private static final int MAX_REPLY_DEPTH = 3;
    private static final int MAX_REPLY_COUNT = 200;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RequestMapping(path = "/discuss/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(@NotBlank(message = "标题不能为空!") String title, @NotBlank(message = "内容不能为空!") String content,
                                  @RequestHeader("X-User-Id") int userId) {
        if (userId == 0) {
            return CommunityUtil.getJSONString(403, "你还没有登录哦!");
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());

        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "发布成功!");
    }

    @RequestMapping(path = "/discuss/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page,
                                  @RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") int userId) {
        // 帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在!");
        }
        model.addAttribute("post", post);
        // 作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态
        int likeStatus = userId == 0 ? 0 :
                likeService.findEntityLikeStatus(userId, ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        // 评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        // 评论: 给帖子的评论
        // 回复: 给评论的评论
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

        // 批量收集所有评论和回复，用于批量查询用户和点赞
        Set<Integer> allCommentIds = new LinkedHashSet<>();
        // parentId -> replies映射，用于构建回复树
        Map<Integer, List<Comment>> repliesByParent = new HashMap<>();

        if (commentList != null) {
            // 收集根评论的ID
            for (Comment comment : commentList) {
                allCommentIds.add(comment.getId());
            }
            // BFS收集所有层级的回复（带深度限制）
            collectAllReplies(commentList, allCommentIds, repliesByParent, 0);
        }

        // 批量收集所有涉及的用户ID
        Set<Integer> allUserIds = new HashSet<>();
        // 根评论的作者
        if (commentList != null) {
            for (Comment comment : commentList) {
                allUserIds.add(comment.getUserId());
            }
        }
        // 所有回复的作者和目标用户
        for (List<Comment> replyList : repliesByParent.values()) {
            for (Comment reply : replyList) {
                allUserIds.add(reply.getUserId());
                if (reply.getTargetId() != 0) {
                    allUserIds.add(reply.getTargetId());
                }
            }
        }

        // 批量查询用户、点赞、回复数
        Map<Integer, User> userMap = userService.findUsersByIds(new ArrayList<>(allUserIds));
        Map<Integer, Long> likeCountMap = likeService.findEntityLikeCounts(ENTITY_TYPE_COMMENT, new ArrayList<>(allCommentIds));
        Map<Integer, Integer> replyCountMap = commentService.findCommentCounts(ENTITY_TYPE_COMMENT, new ArrayList<>(allCommentIds));

        Map<Integer, Integer> likeStatusMap = new HashMap<>();
        if (userId != 0 && !allCommentIds.isEmpty()) {
            likeStatusMap = likeService.findEntityLikeStatuses(
                    userId, ENTITY_TYPE_COMMENT, new ArrayList<>(allCommentIds));
        }

        // 构建评论VO列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> commentVo = buildCommentVo(comment, userMap, likeCountMap, likeStatusMap, replyCountMap, repliesByParent);
                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments", commentVoList);

        if (userId > 0) {
            model.addAttribute("loginUser", userService.findUserById(userId));
        }

        return "/site/discuss-detail";
    }

    // BFS收集所有层级的回复，带深度限制
    private void collectAllReplies(List<Comment> parentComments, Set<Integer> allCommentIds,
                                    Map<Integer, List<Comment>> repliesByParent, int depth) {
        if (depth >= MAX_REPLY_DEPTH || parentComments == null || parentComments.isEmpty()) {
            return;
        }

        List<Comment> nextLevelParents = new ArrayList<>();
        for (Comment parent : parentComments) {
            List<Comment> replies = commentService.findCommentsByEntity(
                    ENTITY_TYPE_COMMENT, parent.getId(), 0, MAX_REPLY_COUNT);
            if (replies != null && !replies.isEmpty()) {
                repliesByParent.put(parent.getId(), replies);
                for (Comment reply : replies) {
                    allCommentIds.add(reply.getId());
                }
                nextLevelParents.addAll(replies);
            }
        }

        collectAllReplies(nextLevelParents, allCommentIds, repliesByParent, depth + 1);
    }

    // 递归构建单个评论VO（使用预查询的批量数据）
    private Map<String, Object> buildCommentVo(Comment comment, Map<Integer, User> userMap,
                                                Map<Integer, Long> likeCountMap, Map<Integer, Integer> likeStatusMap,
                                                Map<Integer, Integer> replyCountMap,
                                                Map<Integer, List<Comment>> repliesByParent) {
        Map<String, Object> commentVo = new HashMap<>();
        commentVo.put("comment", comment);
        commentVo.put("user", userMap.get(comment.getUserId()));
        commentVo.put("likeCount", likeCountMap.getOrDefault(comment.getId(), 0L));
        commentVo.put("likeStatus", likeStatusMap.getOrDefault(comment.getId(), 0));

        // 递归构建回复列表
        List<Comment> replies = repliesByParent.get(comment.getId());
        List<Map<String, Object>> replyVoList = new ArrayList<>();
        if (replies != null) {
            for (Comment reply : replies) {
                Map<String, Object> replyVo = new HashMap<>();
                replyVo.put("reply", reply);
                replyVo.put("user", userMap.get(reply.getUserId()));
                User target = reply.getTargetId() == 0 ? null : userMap.get(reply.getTargetId());
                replyVo.put("target", target);
                replyVo.put("likeCount", likeCountMap.getOrDefault(reply.getId(), 0L));
                replyVo.put("likeStatus", likeStatusMap.getOrDefault(reply.getId(), 0));
                // 递归子回复
                List<Map<String, Object>> subReplys = new ArrayList<>();
                List<Comment> subReplies = repliesByParent.get(reply.getId());
                if (subReplies != null) {
                    for (Comment subReply : subReplies) {
                        subReplys.add(buildCommentVo(subReply, userMap, likeCountMap, likeStatusMap, replyCountMap, repliesByParent));
                    }
                }
                replyVo.put("replys", subReplys);
                replyVo.put("replyCount", replyCountMap.getOrDefault(reply.getId(), 0));

                replyVoList.add(replyVo);
            }
        }
        commentVo.put("replys", replyVoList);
        commentVo.put("replyCount", replyCountMap.getOrDefault(comment.getId(), 0));

        return commentVo;
    }

    // 置顶（切换）
    @RequestMapping(path = "/discuss/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id, @RequestHeader("X-User-Id") int userId) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        int newType = (post != null && post.getType() == 1) ? 0 : 1;
        discussPostService.updateType(id, newType);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, null,
                new HashMap<String, Object>() {{ put("type", newType); }});
    }

    // 加精（切换）
    @RequestMapping(path = "/discuss/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id, @RequestHeader("X-User-Id") int userId) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        int newStatus = (post != null && post.getStatus() == 1) ? 0 : 1;
        discussPostService.updateStatus(id, newStatus);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0, null,
                new HashMap<String, Object>() {{ put("status", newStatus); }});
    }

    // 删除
    @RequestMapping(path = "/discuss/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id, @RequestHeader("X-User-Id") int userId) {
        discussPostService.updateStatus(id, 2);

        // 触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(userId)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

}
