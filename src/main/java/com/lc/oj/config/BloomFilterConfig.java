package com.lc.oj.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.lc.oj.constant.RedisConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 布隆过滤器配置类
 */
@Configuration
public class BloomFilterConfig {

    @Resource
    private StringRedisTemplate template;

    @Bean
    public BloomFilter<String> QuestionBloomFilter() {
        BloomFilter<String> filter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100000, 0.01);
        Set<String> range = template.opsForZSet().range(RedisConstant.QUESTION_LIST_KEY, 0, -1);
        if (range != null) {
            for (String s : range) {
                filter.put(s);
            }
        }
        return filter;
    }
}
