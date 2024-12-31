package com.lc.oj.model.dto.judge;

import lombok.Data;

import java.util.List;

@Data
public class StrategyResponse {

    /**
     * 是否判题成功
     */
    private boolean success;

    /**
     * 总的判题信息，包括消耗的最大时间和内存
     */
    private JudgeInfo judgeInfo;

    /**
     * 每个点的测试信息
     */
    private List<CaseInfo> caseInfoList;
}
