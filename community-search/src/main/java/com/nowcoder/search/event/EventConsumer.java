package com.nowcoder.search.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.feign.PostClient;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.Result;
import com.nowcoder.search.service.ElasticsearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EventConsumer implements CommunityConstant {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private PostClient postClient;

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        Event event = parseEvent(record);
        if (event == null) return;

        DiscussPost post = null;
        String postJson = (String) event.getData().get("postJson");
        if (postJson != null) {
            post = JSONObject.parseObject(postJson, DiscussPost.class);
        }
        if (post == null) {
            try {
                Result result = postClient.findPostById(event.getEntityId());
                Map<String, Object> data = result.getData();
                if (data != null && data.get("post") instanceof Map) {
                    post = JSONObject.parseObject(JSONObject.toJSONString(data.get("post")), DiscussPost.class);
                }
            } catch (Exception e) {
                log.warn("从post服务获取帖子失败, postId={}", event.getEntityId(), e);
            }
        }
        if (post == null) {
            post = new DiscussPost();
            post.setId(event.getEntityId());
            post.setUserId(event.getUserId());
        }
        elasticsearchService.saveDiscussPost(post);
    }

    // 消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        Event event = parseEvent(record);
        if (event == null) return;

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    private Event parseEvent(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空!");
            return null;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误!");
        }
        return event;
    }

}
