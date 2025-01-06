package com.lc.oj.judge;

import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.judge.StrategyResponse;


public interface JudgeStrategy {
    StrategyResponse doJudgeWithStrategy(StrategyRequest strategyRequest);
}
