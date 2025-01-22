package com.lc.oj.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目提交封装类
 *
 * @TableName question
 */
@Data
public class QuestionSubmitCountVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 提交次数
     */
    private Integer submitCount;
    /**
     * 通过次数
     */
    private Integer acceptCount;
    /**
     * 解答错误次数
     */
    private Integer wrongCount;
    /**
     * 时间超限次数
     */
    private Integer timeLimitCount;
    /**
     * 内存超限次数
     */
    private Integer memoryLimitCount;
    /**
     * 编译错误次数
     */
    private Integer compileErrorCount;
    /**
     * 运行错误次数
     */
    private Integer runtimeErrorCount;
}