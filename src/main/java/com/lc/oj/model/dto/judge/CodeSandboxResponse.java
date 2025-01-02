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
    private CaseInfo caseInfo;
}
