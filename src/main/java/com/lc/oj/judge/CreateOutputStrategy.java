package com.lc.oj.judge;

import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.StrategyConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.judge.CaseInfo;
import com.lc.oj.model.dto.judge.CodeSandboxRequest;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.question.JudgeConfig;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.properties.JudgeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Component(StrategyConstant.CREATE_OUTPUT)
public class CreateOutputStrategy extends BaseStrategyAbstract {

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    public CreateOutputStrategy(JudgeProperties judgeProperties) {
        super(judgeProperties);
    }

    @Override
    protected void loadData(List<String> inputList, List<String> outputList, File[] files) throws Exception {
        //找到dir文件夹中的所有.in文件
        for (File inputFile : files) {
            if (!inputFile.isFile() || !inputFile.getName().endsWith(".in")) continue;
            String inputFilePath = inputFile.getPath();
            String prefix = inputFilePath.substring(0, inputFilePath.length() - 3);
            String outputFilePath = prefix + ".out";
            try {
                inputList.add(new String(Files.readAllBytes(Paths.get(inputFilePath))));
                outputList.add(outputFilePath); // 保存输出文件路径，方便后续写入
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败: " + e);
            }
        }
        if (inputList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "无测试数据");
        }
    }

    // 多线程并发造输出数据
    @Override
    protected List<CaseInfo> doJudgeAll(StrategyRequest strategyRequest, List<String> inputList, List<String> outputList) throws Exception {
        String code = strategyRequest.getCode();
        String language = strategyRequest.getLanguage();
        JudgeConfig judgeConfig = strategyRequest.getJudgeConfig();
        List<CaseInfo> caseInfoList = new ArrayList<>();
        List<CompletableFuture<CaseInfo>> futures = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i++) {
            final int index = i;
            CompletableFuture<CaseInfo> future = CompletableFuture.supplyAsync(() -> {
                CodeSandboxRequest codeSandboxRequest = CodeSandboxRequest.builder()
                        .source_code(code)
                        .language_id(languageId.get(language))
                        .compiler_options(compilerOptions.get(language))
                        .cpu_time_limit(judgeConfig.getTimeLimit() * 2 / 1000.0) // 造数据时宽限时间限制
                        .memory_limit(judgeConfig.getMemoryLimit() * 1024)
                        .stdin(inputList.get(index))
                        .build();
                try {
                    return doJudgeOnce(codeSandboxRequest, index, true);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }, executorService);
            futures.add(future);
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(60 * 2, TimeUnit.SECONDS) // 总的判题时间不超过60*2秒
                    .join();
        } catch (Exception e) {
            // 如果超时或有线程抛异常，中断所有线程
            executorService.shutdownNow();
            if (e instanceof CompletionException && e.getCause() instanceof TimeoutException) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评测超时");
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评测失败: " + e.getCause().getMessage());
            }
        }
        // 正常退出，收集判题结果
        for (CompletableFuture<CaseInfo> future : futures) {
            caseInfoList.add(future.join());
        }
        caseInfoList.sort(Comparator.comparing(CaseInfo::getCaseId));
        for (CaseInfo caseInfo : caseInfoList) {
            // 如果有一个点没有通过，就不再继续造数据
            if (!Objects.equals(caseInfo.getJudgeResult(), JudgeResultEnum.ACCEPTED.getValue())) {
                return caseInfoList;
            }
        }
        for (CaseInfo caseInfo : caseInfoList) {
            String output = caseInfo.getExpectOutput();
            String outputFilePath = outputList.get(caseInfo.getCaseId());
            try {
                // 将输出写入文件
                Files.write(Paths.get(outputFilePath), output.getBytes());
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入文件失败: " + e);
            }
        }
        return caseInfoList;
    }
}
