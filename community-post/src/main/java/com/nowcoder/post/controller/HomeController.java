package com.nowcoder.post.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.post.service.LikeService;
import com.nowcoder.post.service.UserService;
import com.nowcoder.post.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = {"/", "/index"}, method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode,
                               @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> list = discussPostService
                .findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            List<Integer> userIds = list.stream().map(DiscussPost::getUserId).distinct().collect(Collectors.toList());
            Map<Integer, com.nowcoder.community.entity.User> userMap = userService.findUsersByIds(userIds);
            List<Integer> postIds = list.stream().map(DiscussPost::getId).collect(Collectors.toList());
            Map<Integer, Long> likeCountMap = likeService.findEntityLikeCounts(ENTITY_TYPE_POST, postIds);

            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("user", userMap.get(post.getUserId()));
                map.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);

        // 从 JWT 中读取当前用户
        if (userId != null) {
            model.addAttribute("loginUser", userService.findUserById(userId));
        }

        return "/index";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}
