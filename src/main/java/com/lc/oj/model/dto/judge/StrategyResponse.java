package com.lc.oj.model.dto.judge;

import lombok.Data;

import java.util.List;

@Data
public class StrategyResponse {

    /**
     * 最大耗时
     */
    private Long maxTime;

    /**
     * 最大内存
     */
    private Long maxMemory;

    /**
     * 判题结果
     */
    private Integer judgeResult;

    /**
     * 每个点的测试信息
     */
    private List<CaseInfo> caseInfoList;
}
