package com.nowcoder.post.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    public Map<Integer, Long> findEntityLikeCounts(int entityType, List<Integer> entityIds) {
        Map<Integer, Long> result = new HashMap<>();
        if (entityIds == null || entityIds.isEmpty()) {
            return result;
        }

        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (Integer entityId : entityIds) {
                    String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                    operations.opsForSet().size(key);
                }
                return null;
            }
        });

        if (results != null) {
            for (int i = 0; i < entityIds.size(); i++) {
                Object r = results.get(i);
                long count = 0;
                if (r instanceof Long) {
                    count = (Long) r;
                } else if (r instanceof Integer) {
                    count = ((Integer) r).longValue();
                }
                result.put(entityIds.get(i), count);
            }
        }

        return result;
    }

    public Map<Integer, Integer> findEntityLikeStatuses(int userId, int entityType, List<Integer> entityIds) {
        Map<Integer, Integer> result = new HashMap<>();
        if (entityIds == null || entityIds.isEmpty()) {
            return result;
        }

        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (Integer entityId : entityIds) {
                    String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                    operations.opsForSet().isMember(key, userId);
                }
                return null;
            }
        });

        if (results != null) {
            for (int i = 0; i < entityIds.size(); i++) {
                Object r = results.get(i);
                int status = (r instanceof Boolean && (Boolean) r) ? 1 : 0;
                result.put(entityIds.get(i), status);
            }
        }

        return result;
    }

}
