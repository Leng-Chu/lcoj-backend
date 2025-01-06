package com.lc.oj.model.dto.judge;

import com.lc.oj.model.dto.question.JudgeConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StrategyRequest {

    /**
     * 代码
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 题号
     */
    private Long num;

    /**
     * 判题配置
     */
    private JudgeConfig judgeConfig;
}
