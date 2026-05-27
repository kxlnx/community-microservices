package com.nowcoder.interact.service;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.feign.PostClient;
import com.nowcoder.community.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DiscussPostService {

    @Autowired
    private PostClient postClient;

    public DiscussPost findDiscussPostById(int id) {
        Result result = postClient.findPostById(id);
        if (result != null && result.getCode() == 0) {
            Map<String, Object> data = result.getData();
            if (data != null && data.get("post") instanceof DiscussPost) {
                return (DiscussPost) data.get("post");
            }
        }
        return null;
    }

    public int incrementCommentCount(int id) {
        postClient.incrementCommentCount(id);
        return 1;
    }

}
