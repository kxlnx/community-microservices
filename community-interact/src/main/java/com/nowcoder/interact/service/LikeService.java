package com.nowcoder.interact.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Lua脚本：原子化 toggle 点赞，无需WATCH重试
    private static final String LIKE_LUA_SCRIPT =
            "local entityKey = KEYS[1]\n" +
            "local userKey = KEYS[2]\n" +
            "local userId = ARGV[1]\n" +
            "if redis.call('sismember', entityKey, userId) == 1 then\n" +
            "    redis.call('srem', entityKey, userId)\n" +
            "    redis.call('decr', userKey)\n" +
            "    return 0\n" +
            "else\n" +
            "    redis.call('sadd', entityKey, userId)\n" +
            "    redis.call('incr', userKey)\n" +
            "    return 1\n" +
            "end";

    // 点赞（Lua 脚本原子执行，高并发下无 WATCH 重试开销）
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LIKE_LUA_SCRIPT, Long.class);
        redisTemplate.execute(script, Arrays.asList(entityLikeKey, userLikeKey), String.valueOf(userId));
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Object obj = redisTemplate.opsForValue().get(userLikeKey);
        Integer count = (obj instanceof Integer) ? (Integer) obj : null;
        return count == null ? 0 : count.intValue();
    }

    // 批量查询实体点赞数
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

    // 批量查询用户对实体的点赞状态
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
