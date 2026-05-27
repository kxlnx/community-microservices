package com.nowcoder.search.service;

import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import java.util.stream.Collectors;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void saveDiscussPosts(List<DiscussPost> posts) {
        discussRepository.saveAll(posts);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();

        Highlight highlight = new Highlight(
                parameters,
                List.of(new HighlightField("title"), new HighlightField("content"))
        );

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m
                        .fields("title", "content")
                        .query(keyword)))
                .withSort(Sort.by(Sort.Direction.DESC, "type"))
                .withSort(Sort.by(Sort.Direction.DESC, "score"))
                .withSort(Sort.by(Sort.Direction.DESC, "createTime"))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightQuery(new HighlightQuery(highlight, DiscussPost.class))
                .build();

        SearchHits<DiscussPost> searchHits = elasticsearchOperations.search(searchQuery, DiscussPost.class);

        for (SearchHit<DiscussPost> hit : searchHits) {
            DiscussPost post = hit.getContent();
            Map<String, List<String>> highlightFields = hit.getHighlightFields();
            if (highlightFields.containsKey("title")) {
                post.setTitle(highlightFields.get("title").get(0));
            }
            if (highlightFields.containsKey("content")) {
                post.setContent(highlightFields.get("content").get(0));
            }
        }

        List<DiscussPost> posts = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(posts, PageRequest.of(current, limit), searchHits.getTotalHits());
    }

}
