package com.nowcoder.search.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.Result;
import com.nowcoder.search.service.ElasticsearchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@Validated
public class SearchController implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    // /api/search?keyword=xxx
    @GetMapping
    public Result search(@NotBlank(message = "请输入搜索关键词!") String keyword, Page page) {
        // 搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        Map<String, Object> data = new HashMap<>();
        if (searchResult != null && searchResult.hasContent()) {
            data.put("discussPosts", searchResult.getContent());
            data.put("total", searchResult.getTotalElements());
            data.put("current", page.getCurrent());
            data.put("limit", page.getLimit());
        } else {
            data.put("discussPosts", List.of());
            data.put("total", 0L);
            data.put("current", page.getCurrent());
            data.put("limit", page.getLimit());
        }
        data.put("keyword", keyword);

        return Result.success(data);
    }

}
