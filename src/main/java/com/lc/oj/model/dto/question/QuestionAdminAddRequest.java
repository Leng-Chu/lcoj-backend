package com.lc.oj.model.dto.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 */
@Data
public class QuestionAdminAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 题号
     */
    private Long num;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 标签列表
     */
    private String tags;
    /**
     * 题目标程
     */
    private String answer;
    /**
     * 标程语言
     */
    private String language;
    /**
     * 样例
     */
    private String sampleCase;
    /**
     * 判题配置
     */
    private String judgeConfig;

    private Long userId;

    private String userName;
}