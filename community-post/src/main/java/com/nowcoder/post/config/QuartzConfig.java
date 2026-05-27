package com.nowcoder.post.config;

import com.nowcoder.post.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * Quartz定时任务配置类
 * 负责配置和管理系统中的定时任务
 * 使用Spring的FactoryBean简化Quartz的配置过程
 *
 * 配置流程：配置 -> 数据库 -> 调用
 */
@Configuration
public class QuartzConfig {

    /**
     * FactoryBean的作用：简化Bean的实例化过程
     * 1. 通过FactoryBean封装Bean的实例化过程
     * 2. 将FactoryBean装配到Spring容器里
     * 3. 将FactoryBean注入给其他的Bean
     * 4. 该Bean得到的是FactoryBean所管理的对象实例
     */

    /**
     * 配置帖子分数刷新任务的JobDetail
     * 该任务负责定期刷新帖子的热度分数
     *
     * @return JobDetailFactoryBean实例
     */
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        // 设置任务执行类
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        // 设置任务名称
        factoryBean.setName("postScoreRefreshJob");
        // 设置任务组名
        factoryBean.setGroup("communityJobGroup");
        // 设置任务持久化
        factoryBean.setDurability(true);
        // 设置任务可恢复
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    /**
     * 配置帖子分数刷新任务的触发器
     * 该触发器每5分钟执行一次帖子分数刷新任务
     *
     * @param postScoreRefreshJobDetail 关联的帖子分数刷新JobDetail
     * @return SimpleTriggerFactoryBean实例
     */
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        // 设置关联的JobDetail
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        // 设置触发器名称
        factoryBean.setName("postScoreRefreshTrigger");
        // 设置触发器组名
        factoryBean.setGroup("communityTriggerGroup");
        // 设置重复间隔（5分钟：1000ms * 60 * 5 = 300000ms）
        factoryBean.setRepeatInterval(1000 * 60 * 5);
        // 设置任务数据映射
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}
