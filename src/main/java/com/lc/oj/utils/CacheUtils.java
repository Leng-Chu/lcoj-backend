package com.lc.oj.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.model.entity.Question;
import com.lc.oj.service.IQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.lc.oj.constant.RedisConstant.LOCK_TTL;

@Slf4j
@Component
public class CacheUtils {

    @Resource
    private StringRedisTemplate template;
    @Resource
    private IQuestionService questionService;
    private BloomFilter<String> questionFilter;


    @PostConstruct
    public void init() {
        questionFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100000, 0.01);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id");
        List<Question> questions = questionService.list(queryWrapper);
        if (questions != null) {
            for (Question q : questions) {
                questionFilter.put(String.valueOf(q.getId()));
            }
        }
    }

    public void addQuestionIdToBloomFilter(Long questionId) {
        questionFilter.put(String.valueOf(questionId));
    }

    private boolean tryLock(String key) {
        Boolean flag = template.opsForValue().setIfAbsent(key, "lock", LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        template.delete(key);
    }

    //带TTL的缓存
    private void set(String key, Object value, Long time) {
        // time加上一个随机值，防止缓存雪崩
        time = time + (long) (Math.random() * 1000);
        template.opsForValue().set(key, JSONUtil.toJsonStr(value), time, TimeUnit.SECONDS);
    }

    //用布隆过滤器和缓存空对象解决缓存穿透问题，同时用互斥锁解决缓存击穿问题
    public <R, ID> R query(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack) {
        String key = keyPrefix + id;
        //1.从Redis查询缓存
        String json = template.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3.存在，直接返回
            log.info("缓存查询成功，key={}", key);
            return JSONUtil.toBean(json, type);
        }
        //上面是有值的情况，下面是无值的情况
        //A：如果是题目，先检查布隆过滤器中是否存在该题目
        if (RedisConstant.QUESTION_CACHE_KEY.equals(keyPrefix)) {
            boolean b = questionFilter.mightContain(id + "");
            if (!b) { // 如果不存在，证明没有这个数据
                log.info("布隆过滤器中不存在，id={}", id);
                return null;
            }
        }
        //B：空字符串，证明缓存了空数据
        if (json != null) {
            return null;
        }
        //C：null，证明没有缓存，需要重建
        //4.基于互斥锁实现缓存重建
        //4.1 获取互斥锁
        String lockKey = "lock:" + keyPrefix + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2 判断是否获取成功
            if (!isLock) {
                //4.3 失败，则休眠并重试
                Thread.sleep(100);
                return query(keyPrefix, id, type, dbFallBack);
            }
            //4.4 获取互斥锁成功，根据id查询数据库
            r = dbFallBack.apply(id);
            if (r == null) {
                //5.数据库查询失败，缓存空对象
                set(key, "", RedisConstant.CACHE_NULL_TTL);
            } else {
                //6.存在，写入Redis
                set(key, r, RedisConstant.CACHE_TTL);
                log.info("写入缓存，key={}", key);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //7.释放互斥锁
            unLock(lockKey);
        }
        //8.返回
        return r;
    }


}
