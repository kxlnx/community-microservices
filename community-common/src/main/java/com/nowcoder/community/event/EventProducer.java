package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    private static final Logger logger = LoggerFactory.getLogger(EventProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void fireEvent(Event event) {
        String topic = event.getTopic();
        String message = JSONObject.toJSONString(event);
        kafkaTemplate.send(topic, message).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Kafka 发送失败: topic={}, message={}", topic, message, ex);
            }
        });
    }

}
