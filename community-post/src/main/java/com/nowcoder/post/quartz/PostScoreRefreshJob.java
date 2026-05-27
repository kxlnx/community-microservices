package com.nowcoder.post.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.post.service.ElasticsearchService;
import com.nowcoder.post.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.post.service.DiscussPostService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 帖子分数刷新任务
 * 定时任务，负责计算和更新帖子的热度分数
 * 实现Quartz的Job接口，每5分钟执行一次
 */
@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    // Redis操作模板，用于处理需要刷新分数的帖子集合
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 帖子服务，用于查询和更新帖子信息
    @Autowired
    private DiscussPostService discussPostService;

    // 点赞服务，用于获取帖子的点赞数量
    @Autowired
    private LikeService likeService;

    // Elasticsearch服务，用于同步搜索数据
    @Autowired
    private ElasticsearchService elasticsearchService;

    // 牛客纪元
    private static final long EPOCH_MILLIS = LocalDateTime.of(2014, 8, 1, 0, 0, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    /**
     * 任务执行方法：批量加载帖子、批量查点赞数、批量写ES，减少N倍往返
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations<String, Object> operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: {}", operations.size());

        // 1. 从 Redis 弹出所有待刷新的帖子ID
        List<Integer> postIds = new ArrayList<>();
        while (operations.size() > 0) {
            Object popped = operations.pop();
            if (popped instanceof Integer) {
                postIds.add((Integer) popped);
            }
        }

        if (postIds.isEmpty()) {
            logger.info("[任务结束] 没有有效的帖子!");
            return;
        }

        // 2. 批量从 MySQL 加载帖子（一次查询替代 N 次）
        List<DiscussPost> posts = discussPostService.findDiscussPostsByIds(postIds);

        // 3. 批量从 Redis 查点赞数（一次 pipeline 替代 N 次 SCARD）
        Map<Integer, Long> likeCountMap = likeService.findEntityLikeCounts(ENTITY_TYPE_POST, postIds);

        // 4. 计算分数并更新
        List<DiscussPost> updatedPosts = new ArrayList<>();
        for (DiscussPost post : posts) {
            if (post == null) continue;

            boolean wonderful = post.getStatus() == 1;
            int commentCount = post.getCommentCount();
            long likeCount = likeCountMap.getOrDefault(post.getId(), 0L);

            double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
            double score = Math.log10(Math.max(w, 1))
                    + (post.getCreateTime().getTime() - EPOCH_MILLIS) / (1000 * 3600 * 24);

            discussPostService.updateScore(post.getId(), score);
            post.setScore(score);
            updatedPosts.add(post);
        }

        // 5. 批量写 ES（一次 saveAll 替代 N 次 HTTP 调用）
        if (!updatedPosts.isEmpty()) {
            elasticsearchService.saveDiscussPosts(updatedPosts);
        }

        logger.info("[任务结束] 帖子分数刷新完毕! 共处理: {}", updatedPosts.size());
    }

}
