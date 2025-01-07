package com.lc.oj.judge;

import com.lc.oj.common.ErrorCode;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.judge.CaseInfo;
import com.lc.oj.model.dto.judge.CodeSandboxRequest;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.judge.StrategyResponse;
import com.lc.oj.model.dto.question.JudgeConfig;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.properties.JudgeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
public class NormalStrategy extends BaseStrategyAbstract {

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public NormalStrategy(JudgeProperties judgeProperties) {
        super(judgeProperties);
    }

    @Override
    protected StrategyResponse doJudgeAll(StrategyRequest strategyRequest, List<String> inputList, List<String> outputList) throws Exception {
        String code = strategyRequest.getCode();
        String language = strategyRequest.getLanguage();
        JudgeConfig judgeConfig = strategyRequest.getJudgeConfig();
        StrategyResponse strategyResponse = new StrategyResponse();
        strategyResponse.setCaseInfoList(new ArrayList<>());
        List<Future<CaseInfo>> futures = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i++) {
            final int index = i;
            Callable<CaseInfo> task = () -> {
                CodeSandboxRequest codeSandboxRequest = CodeSandboxRequest.builder()
                        .source_code(code)
                        .language_id(languageId.get(language))
                        .compiler_options(compilerOptions.get(language))
                        .cpu_time_limit(judgeConfig.getTimeLimit() / 1000.0)
                        .memory_limit(judgeConfig.getMemoryLimit() * 1024)
                        .stdin(inputList.get(index))
                        .expected_output(outputList.get(index))
                        .build();
                return doJudgeOnce(codeSandboxRequest, index);
            };
            futures.add(executorService.submit(task));
        }
        for (Future<CaseInfo> future : futures) {
            try {
                CaseInfo caseInfo = future.get();
                //log.info("caseInfo: {}", caseInfo);
                strategyResponse.getCaseInfoList().add(caseInfo);
                if (caseInfo.getTime() != null) {
                    if (strategyResponse.getMaxTime() == null) {
                        strategyResponse.setMaxTime(caseInfo.getTime());
                    } else {
                        strategyResponse.setMaxTime(Math.max(strategyResponse.getMaxTime(), caseInfo.getTime()));
                    }
                }
                if (caseInfo.getMemory() != null) {
                    if (strategyResponse.getMaxMemory() == null) {
                        strategyResponse.setMaxMemory(caseInfo.getMemory());
                    } else {
                        strategyResponse.setMaxMemory(Math.max(strategyResponse.getMaxMemory(), caseInfo.getMemory()));
                    }
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用代码沙箱异常: " + e);
            }
        }
        strategyResponse.getCaseInfoList().sort(Comparator.comparing(CaseInfo::getCaseId));
        for (CaseInfo caseInfo : strategyResponse.getCaseInfoList()) {
            if (strategyResponse.getJudgeResult() == null
                    && !Objects.equals(caseInfo.getJudgeResult(), JudgeResultEnum.ACCEPTED.getValue())) {
                strategyResponse.setJudgeResult(caseInfo.getJudgeResult());
            }
        }
        if (strategyResponse.getJudgeResult() == null) {
            strategyResponse.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
        }
        log.info("评测完毕，结果: {}", strategyResponse.getJudgeResult());
        return strategyResponse;
    }
}
