package com.nowcoder.post.controller;

import com.alibaba.fastjson.JSON;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.feign.SearchClient;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.Result;
import com.nowcoder.post.service.LikeService;
import com.nowcoder.post.service.UserService;
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
public class SearchController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private SearchClient searchClient;

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(@RequestParam("keyword") String keyword, Page page, Model model,
                         @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Result result = searchClient.search(keyword, page.getCurrent(), page.getLimit());
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            Map<String, Object> data = result.getData();
            long total = ((Number) data.getOrDefault("total", 0)).longValue();
            List<DiscussPost> rawPosts = castList(data.get("discussPosts"));

            List<Map<String, Object>> discussPosts = new ArrayList<>();
            if (rawPosts != null && !rawPosts.isEmpty()) {
                List<Integer> userIds = rawPosts.stream().map(DiscussPost::getUserId).distinct().collect(java.util.stream.Collectors.toList());
                Map<Integer, com.nowcoder.community.entity.User> userMap = userService.findUsersByIds(userIds);
                List<Integer> postIds = rawPosts.stream().map(DiscussPost::getId).collect(java.util.stream.Collectors.toList());
                Map<Integer, Long> likeCountMap = likeService.findEntityLikeCounts(ENTITY_TYPE_POST, postIds);

                for (DiscussPost post : rawPosts) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("post", post);
                    map.put("user", userMap.get(post.getUserId()));
                    map.put("likeCount", likeCountMap.getOrDefault(post.getId(), 0L));
                    discussPosts.add(map);
                }
            }
            model.addAttribute("discussPosts", discussPosts);
            page.setRows((int) total);
        } else {
            model.addAttribute("discussPosts", Collections.emptyList());
            page.setRows(0);
        }
        if (userId != null) {
            model.addAttribute("loginUser", userService.findUserById(userId));
        }
        page.setPath("/search?keyword=" + keyword);
        model.addAttribute("keyword", keyword);
        return "/site/search";
    }

    @SuppressWarnings("unchecked")
    private List<DiscussPost> castList(Object obj) {
        if (obj instanceof List) {
            return ((List<Object>) obj).stream()
                    .map(item -> JSON.parseObject(JSON.toJSONString(item), DiscussPost.class))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
