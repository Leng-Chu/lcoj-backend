package com.lc.oj.model.dto.judge;

import lombok.Data;

@Data
public class CodeSandboxResponse {

    /**
     * 是否判题成功
     */
    private boolean success;

    /**
     * 判题信息
     */
    private JudgeInfo judgeInfo;

    /**
     * 如果没有通过该测试点，将错误信息储存在caseInfo中
     */
    private CaseInfo caseInfo;
}
