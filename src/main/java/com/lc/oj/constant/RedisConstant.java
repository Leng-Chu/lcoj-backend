package com.lc.oj.constant;

public interface RedisConstant {

    // string类型
    String SUBMIT_LOCK_KEY = "submit:lock:"; // key为用户id，用户提交了一道题目后，加锁，防止用户重复提交
    String SUBMIT_REJUDGE_KEY = "submit:rejudge:"; // key为提交id，表明某提交记录正在重判
    String SUBMIT_CACHE_KEY = "submit:cache:"; // key为提交id，缓存提交详情
    String QUESTION_CACHE_KEY = "question:cache:"; // key为题目id，缓存题目详情

    // set类型
    String QUESTION_ACCEPT_KEY = "question:accept:"; // key为用户名，用set存储用户通过的题目id
    String QUESTION_FAIL_KEY = "question:fail:"; // key为用户名，用set存储用户尝试了未通过的题目id

    // zset类型
    String QUESTION_LIST_KEY = "question:list"; // value为题目id，score为题目num
    String SUBMIT_LIST_KEY = "submit:list"; // value为提交id，score为提交时间
    String USER_RANK_KEY = "rank:accept"; // value为用户名，score为用户通过题目数

    // ttl，单位为秒
    Long LOCK_TTL = 10L;
    Long CACHE_NULL_TTL = 120L;
    Long CACHE_TTL = 24 * 60 * 60L;
}
