package com.lc.oj.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建请求
 */
@Data
public class QuestionAddRequest implements Serializable {

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
    private List<String> tags;
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
    private List<SampleCase> sampleCase;
    /**
     * 判题配置
     */
    private JudgeConfig judgeConfig;
}