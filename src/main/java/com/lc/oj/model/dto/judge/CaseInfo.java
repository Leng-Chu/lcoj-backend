package com.lc.oj.model.dto.judge;

import lombok.Data;

/**
 * 每个测试点的具体信息
 * 如果judgeResult为2~7，那么message有值
 * 如果judgeResult为Wrong Answer，那么input, expectOutput, wrongOutput有值
 * 如果judgeResult为Runtime Error，那么input有值
 * 只返回输入输出都不超过500KB的测试数据
 */
@Data
public class CaseInfo {

    /**
     * 判题结果
     */
    private Integer judgeResult;

    /**
     * 消耗时间
     */
    private Long time;

    /**
     * 消耗内存
     */
    private Long memory;

    /**
     * 输入用例
     */
    private String input;

    /**
     * 输出用例
     */
    private String expectOutput;

    /**
     * 错误输出
     */
    private String wrongOutput;

    /**
     * 错误信息
     */
    private String message;

}
