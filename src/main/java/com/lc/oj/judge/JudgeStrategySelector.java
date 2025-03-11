package com.lc.oj.judge;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class JudgeStrategySelector {

    @Resource
    private Map<String, JudgeStrategy> selectorMap;

    public JudgeStrategy select(String type) {
        return selectorMap.get(type);
    }
}
