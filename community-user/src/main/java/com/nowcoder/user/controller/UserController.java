package com.nowcoder.user.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.feign.CommentClient;
import com.nowcoder.community.feign.PostClient;
import com.nowcoder.community.util.BusinessException;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.Result;
import com.nowcoder.user.service.FollowService;
import com.nowcoder.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private FollowService followService;

    @Autowired
    private PostClient postClient;

    @Autowired
    private CommentClient commentClient;

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model,
                                  @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new BusinessException(404, "该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (currentUserId != null) {
            hasFollowed = followService.hasFollowed(currentUserId, ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        if (currentUserId != null) {
            model.addAttribute("loginUser", userService.findUserById(currentUserId));
        }

        // 帖子数量和数据
        try {
            Result postCountResult = postClient.countByUserId(userId);
            int postCount = ((Number) postCountResult.getData().get("count")).intValue();
            model.addAttribute("postCount", postCount);
            if (postCount > 0) {
                Result postsResult = postClient.findByUserId(userId, 0, postCount);
                model.addAttribute("posts", postsResult.getData().get("posts"));
            }
        } catch (Exception ignored) {}

        // 回复数量和数据
        try {
            Result replyCountResult = commentClient.countByUserId(userId);
            int replyCount = ((Number) replyCountResult.getData().get("count")).intValue();
            model.addAttribute("replyCount", replyCount);
            if (replyCount > 0) {
                Result repliesResult = commentClient.findByUserId(userId, 0, replyCount);
                model.addAttribute("replies", repliesResult.getData().get("comments"));
            }
        } catch (Exception ignored) {}

        // 获赞数
        try {
            Result likeResult = commentClient.findUserLikeCount(userId);
            model.addAttribute("likeCount", likeResult.getData().get("likeCount"));
        } catch (Exception ignored) {}

        return "/site/profile";
    }

    // 批量获取用户信息（供Feign调用）
    @PostMapping("/batch")
    @ResponseBody
    public Map<Integer, Object> batchGetUsers(@RequestBody List<Integer> ids) {
        Map<Integer, User> userMap = userService.findUsersByIds(ids);
        return new HashMap<>(userMap);
    }

    // 我的帖子
    @RequestMapping(path = "/my-post", method = RequestMethod.GET)
    public String getMyPosts(Model model, @RequestHeader("X-User-Id") int userId) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("loginUser", user);
        // 从post服务获取帖子数据
        try {
            Result countResult = postClient.countByUserId(userId);
            int postCount = (int) countResult.getData().get("count");
            model.addAttribute("postCount", postCount);
            Result postsResult = postClient.findByUserId(userId, 0, postCount);
            model.addAttribute("posts", postsResult.getData().get("posts"));
        } catch (Exception e) {
            model.addAttribute("postCount", 0);
        }
        return "/site/my-post";
    }

    // 我的回复
    @RequestMapping(path = "/my-reply", method = RequestMethod.GET)
    public String getMyReplies(Model model, @RequestHeader("X-User-Id") int userId) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("loginUser", user);
        // 从interact服务获取回复数据
        try {
            Result countResult = commentClient.countByUserId(userId);
            int replyCount = (int) countResult.getData().get("count");
            model.addAttribute("replyCount", replyCount);
            Result repliesResult = commentClient.findByUserId(userId, 0, replyCount);
            model.addAttribute("replies", repliesResult.getData().get("comments"));
        } catch (Exception e) {
            model.addAttribute("replyCount", 0);
        }
        return "/site/my-reply";
    }

    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model, @RequestHeader("X-User-Id") int userId) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("loginUser", user);
        return "/site/setting";
    }

    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(@RequestParam("headerUrl") String headerUrl,
                                  @RequestHeader("X-User-Id") int userId) {
        userService.updateHeader(userId, headerUrl);
        return CommunityUtil.getJSONString(0, "修改头像成功");
    }

}
