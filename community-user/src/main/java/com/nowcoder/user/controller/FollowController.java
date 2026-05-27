package com.nowcoder.user.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.BusinessException;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.user.service.FollowService;
import com.nowcoder.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId,
                         @RequestHeader("X-User-Id") int userId) {

        followService.follow(userId, entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(userId)
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId,
                           @RequestHeader("X-User-Id") int userId) {

        followService.unfollow(userId, entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注!");
    }

    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model,
                                @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new BusinessException(404, "该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null && !userList.isEmpty()) {
            // 批量查询关注状态
            List<Integer> followeeIds = new ArrayList<>();
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                if (u != null) {
                    followeeIds.add(u.getId());
                }
            }
            Map<Integer, Boolean> hasFollowedMap = null;
            if (currentUserId != null && !followeeIds.isEmpty()) {
                hasFollowedMap = followService.hasFollowedBatch(
                        currentUserId, ENTITY_TYPE_USER, followeeIds);
            }

            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                if (u != null && hasFollowedMap != null) {
                    map.put("hasFollowed", hasFollowedMap.getOrDefault(u.getId(), false));
                } else {
                    map.put("hasFollowed", false);
                }
            }
        }
        model.addAttribute("users", userList);
        if (currentUserId != null) {
            model.addAttribute("loginUser", userService.findUserById(currentUserId));
        }

        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model,
                                @RequestHeader(value = "X-User-Id", required = false) Integer currentUserId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new BusinessException(404, "该用户不存在!");
        }
        model.addAttribute("user", user);

        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null && !userList.isEmpty()) {
            // 批量查询关注状态
            List<Integer> followerIds = new ArrayList<>();
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                if (u != null) {
                    followerIds.add(u.getId());
                }
            }
            Map<Integer, Boolean> hasFollowedMap = null;
            if (currentUserId != null && !followerIds.isEmpty()) {
                hasFollowedMap = followService.hasFollowedBatch(
                        currentUserId, ENTITY_TYPE_USER, followerIds);
            }

            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                if (u != null && hasFollowedMap != null) {
                    map.put("hasFollowed", hasFollowedMap.getOrDefault(u.getId(), false));
                } else {
                    map.put("hasFollowed", false);
                }
            }
        }
        model.addAttribute("users", userList);
        if (currentUserId != null) {
            model.addAttribute("loginUser", userService.findUserById(currentUserId));
        }

        return "/site/follower";
    }

}
