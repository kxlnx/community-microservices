package com.nowcoder.post.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.feign.UserClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public User findUserById(int id) {
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    public Map<Integer, User> findUsersByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }

        List<Integer> distinctIds = ids.stream().distinct().collect(Collectors.toList());

        List<String> redisKeys = distinctIds.stream()
                .map(RedisKeyUtil::getUserKey)
                .collect(Collectors.toList());
        List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(redisKeys);

        Map<Integer, User> result = new HashMap<>();
        List<Integer> missedIds = new ArrayList<>();

        for (int i = 0; i < distinctIds.size(); i++) {
            Integer id = distinctIds.get(i);
            Object cached = cachedUsers != null ? cachedUsers.get(i) : null;
            if (cached instanceof User) {
                result.put(id, (User) cached);
            } else {
                missedIds.add(id);
            }
        }

        if (!missedIds.isEmpty()) {
            Map<Integer, User> remoteUsers = userClient.findUsersByIds(missedIds);
            if (remoteUsers != null) {
                for (Map.Entry<Integer, User> entry : remoteUsers.entrySet()) {
                    User user = entry.getValue();
                    if (user != null) {
                        result.put(entry.getKey(), user);
                        String redisKey = RedisKeyUtil.getUserKey(entry.getKey());
                        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
                    }
                }
            }
            for (Integer missedId : missedIds) {
                if (!result.containsKey(missedId)) {
                    result.put(missedId, null);
                }
            }
        }

        return result;
    }

    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    private User initCache(int userId) {
        User user = userClient.findUserById(userId);
        if (user != null) {
            String redisKey = RedisKeyUtil.getUserKey(userId);
            redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        }
        return user;
    }

}
