package com.lc.oj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${lcoj.threadpool.core-pool-size}")
    private int corePoolSize;
    @Value("${lcoj.threadpool.max-pool-size}")
    private int maxPoolSize;
    @Value("${lcoj.threadpool.queue-capacity}")
    private int queueCapacity;
    @Value("${lcoj.threadpool.keep-alive-seconds}")
    private int keepAliveSeconds;

    @Bean(name = "judgeExecutor")
    public Executor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(corePoolSize);
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        // 线程存活时间
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程名前缀
        executor.setThreadNamePrefix("Judge-");
        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}