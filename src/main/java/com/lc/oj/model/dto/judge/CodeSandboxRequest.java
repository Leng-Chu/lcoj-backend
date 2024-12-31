package com.lc.oj.model.dto.judge;

import com.lc.oj.model.dto.question.JudgeConfig;
import lombok.Data;

@Data
public class CodeSandboxRequest {

    /**
     * 代码
     */
    private String code;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 输入数据
     */
    private String input;

    /**
     * 预期输出
     */
    private String output;

    /**
     * 判题配置
     */
    private JudgeConfig judgeConfig;

    /**
     * 是否需要返回错误数据的具体内容
     */
    private boolean needData;
}
