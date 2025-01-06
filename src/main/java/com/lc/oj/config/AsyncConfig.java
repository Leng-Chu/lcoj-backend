package com.lc.oj.config;

import com.lc.oj.properties.ThreadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.Resource;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Resource
    private ThreadProperties threadProperties;

    @Bean(name = "judgeExecutor")
    public Executor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(threadProperties.getCorePoolSize());
        // 最大线程数
        executor.setMaxPoolSize(threadProperties.getMaxPoolSize());
        // 队列容量
        executor.setQueueCapacity(threadProperties.getQueueCapacity());
        // 线程存活时间
        executor.setKeepAliveSeconds(threadProperties.getKeepAliveSeconds());
        // 线程名前缀
        executor.setThreadNamePrefix("judge-");
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}