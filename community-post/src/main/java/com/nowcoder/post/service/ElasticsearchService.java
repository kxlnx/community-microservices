package com.nowcoder.post.service;

import com.nowcoder.post.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void saveDiscussPosts(List<DiscussPost> posts) {
        discussRepository.saveAll(posts);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

}
