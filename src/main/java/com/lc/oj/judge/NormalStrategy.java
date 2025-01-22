package com.lc.oj.judge;

import com.lc.oj.common.ErrorCode;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.judge.CaseInfo;
import com.lc.oj.model.dto.judge.CodeSandboxRequest;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.question.JudgeConfig;
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
import java.util.concurrent.*;

@Slf4j
@Component
public class NormalStrategy extends BaseStrategyAbstract {

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public NormalStrategy(JudgeProperties judgeProperties) {
        super(judgeProperties);
    }

    @Override
    protected void loadData(List<String> inputList, List<String> outputList, File[] files) throws Exception {
        //遍历dir文件夹中的所有.in文件，并找到同名的.out或.ans文件，如果.out和.ans都存在，优先使用.out文件
        for (File inputFile : files) {
            if (!inputFile.isFile() || !inputFile.getName().endsWith(".in")) continue;
            String inputFilePath = inputFile.getPath();
            String prefix = inputFilePath.substring(0, inputFilePath.length() - 3);
            String outputFilePath = prefix + ".out";
            File outFile = new File(outputFilePath);
            // 优先使用.out文件
            if (!outFile.exists()) {
                outputFilePath = prefix + ".ans";
                outFile = new File(outputFilePath);
                if (!outFile.exists()) continue;
            }
            try {
                inputList.add(new String(Files.readAllBytes(Paths.get(inputFilePath))));
                outputList.add(new String(Files.readAllBytes(Paths.get(outputFilePath))));
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败: " + e);
            }
        }
        if (inputList.isEmpty() || outputList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "无测试数据");
        }
    }

    // 多线程并行评测所有数据
    @Override
    protected List<CaseInfo> doJudgeAll(StrategyRequest strategyRequest, List<String> inputList, List<String> outputList) throws Exception {
        String code = strategyRequest.getCode();
        String language = strategyRequest.getLanguage();
        JudgeConfig judgeConfig = strategyRequest.getJudgeConfig();
        List<CaseInfo> caseInfoList = new ArrayList<>();
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
                return doJudgeOnce(codeSandboxRequest, index, false);
            };
            futures.add(executorService.submit(task));
        }
        for (Future<CaseInfo> future : futures) {
            CaseInfo caseInfo = null;
            try {
                caseInfo = future.get(60, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评测超时");
            }
            caseInfoList.add(caseInfo);
        }
        caseInfoList.sort(Comparator.comparing(CaseInfo::getCaseId));
        return caseInfoList;
    }
}
